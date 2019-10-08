/*
 * Copyright 2019 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package internal.nbbrd.service.definition;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import internal.nbbrd.service.ModuleInfoEntries;
import internal.nbbrd.service.ProcessorUtil;
import internal.nbbrd.service.TypeFactory;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 *
 * @author Philippe Charles
 */
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes({"nbbrd.service.ServiceDefinition"})
public final class ServiceDefinitionProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//        if (roundEnv.processingOver()) {
//            return false;
//        }

        List<ServiceLoaderGenerator> allGenerators = collect(annotations, roundEnv);

        if (!allGenerators.isEmpty()) {
            checkModuleInfo(allGenerators);

            List<ServiceLoaderGenerator> validGenerators = allGenerators
                    .stream()
                    .filter(this::check)
                    .collect(Collectors.toList());

            process(validGenerators);
        }

        return true;
    }

    private List<ServiceLoaderGenerator> collect(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return annotations.stream()
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream)
                .map(TypeElement.class::cast)
                .map(ServiceLoaderGenerator::of)
                .collect(Collectors.toList());
    }

    private boolean check(ServiceLoaderGenerator generator) {
        Types types = processingEnv.getTypeUtils();
        TypeElement service = asTypeElement(generator.getServiceType());

        if (!checkFallback(generator, service, types)) {
            return false;
        }
        if (!checkPreprocessor(generator, service, types)) {
            return false;
        }
        if (!checkMutability(generator, service, types)) {
            return false;
        }
        return true;
    }

    private boolean checkFallback(ServiceLoaderGenerator generator, TypeElement service, Types types) {
        switch (generator.getQuantifier()) {
            case SINGLE:
                if (!generator.getFallbackType().isPresent()) {
                    warn(service, String.format("Missing fallback for service '%1$s'", service));
                }
                break;
            case MULTIPLE:
            case OPTIONAL:
                if (generator.getFallbackType().isPresent()) {
                    warn(service, String.format("Useless fallback for service '%1$s'", service));
                }
                break;
        }

        if (generator.getFallbackType().isPresent()) {
            TypeMirror fallback = generator.getFallbackType().get();

            if (!types.isAssignable(fallback, types.erasure(service.asType()))) {
                error(service, String.format("Fallback '%1$s' doesn't extend nor implement service '%2$s'", fallback, service));
                return false;
            }

            if (!checkFactories(service, fallback)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkPreprocessor(ServiceLoaderGenerator generator, TypeElement service, Types types) {
        if (generator.getPreprocessorType().isPresent()) {
            TypeMirror preprocessor = generator.getPreprocessorType().get();

            DeclaredType streamOf = types.getDeclaredType(asTypeElement(Stream.class), service.asType());
            DeclaredType unaryOperatorOf = types.getDeclaredType(asTypeElement(UnaryOperator.class), streamOf);
            if (!types.isAssignable(preprocessor, unaryOperatorOf)) {
                error(service, String.format("Preprocessor '%1$s' doesn't extend nor implement 'UnaryOperator<Stream<%2$s>>'", preprocessor, service));
                return false;
            }

            if (!checkFactories(service, preprocessor)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkMutability(ServiceLoaderGenerator generator, TypeElement service, Types types) {
        if (generator.getLoaderKind() == LoaderKind.UNSAFE_MUTABLE) {
            warn(service, String.format("Thread-unsafe singleton for '%1$s'", service));
        }
        return true;
    }

    private boolean checkFactories(TypeElement annotatedElement, TypeMirror type) {
        List<TypeFactory> factories = TypeFactory.of(processingEnv.getTypeUtils(), asTypeElement(type));
        if (!TypeFactory.canSelect(factories)) {
            error(annotatedElement, String.format("Don't know how to create '%1$s'", type));
            return false;
        }
        return true;
    }

    private void checkModuleInfo(List<ServiceLoaderGenerator> generators) {
        List<String> serviceTypes = generators.stream()
                .map(generator -> generator.getServiceType().toString())
                .collect(Collectors.toList());

        try {
            Optional<ModuleInfoEntries> entries = ModuleInfoEntries.parse(processingEnv.getFiler());

            entries.map(ModuleInfoEntries::getUsages).ifPresent(usages -> {
                serviceTypes
                        .stream()
                        .filter(serviceType -> (!usages.contains(serviceType)))
                        .map(this::asTypeElement)
                        .forEachOrdered(ref -> error(ref, "Missing module-info.java 'uses' directive for '" + ref + "'"));

//                uses
//                        .stream()
//                        .filter(use -> (!serviceTypes.contains(use)))
//                        .map(this::asTypeElement)
//                        .forEachOrdered(ref -> error(ref, "Missing annotation for '" + ref + "'"));
            });

        } catch (IOException ex) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
    }

    private void process(List<ServiceLoaderGenerator> generators) {
        generators
                .stream()
                .collect(Collectors.groupingBy(o -> resolveLoaderName(o).topLevelClassName()))
                .forEach(this::process);
    }

    private void process(ClassName top, List<ServiceLoaderGenerator> generators) {
        JavaFile file;
        if (generators.size() == 1 && top.equals(resolveLoaderName(generators.get(0)))) {
            ServiceLoaderGenerator generator = generators.get(0);
            file = JavaFile.builder(top.packageName(), generator.generate(top.simpleName(), this::selectTypeFactory)).build();
        } else {
            TypeSpec.Builder nestedTypes = TypeSpec.classBuilder(top)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            generators
                    .stream()
                    .map(generator -> generator.generate(resolveLoaderName(generator).simpleName(), this::selectTypeFactory))
                    .map(o -> o.toBuilder().addModifiers(Modifier.STATIC).build())
                    .forEach(nestedTypes::addType);
            file = JavaFile.builder(top.packageName(), nestedTypes.build()).build();
        }
        ProcessorUtil.write(processingEnv, file);
    }

    private TypeFactory selectTypeFactory(TypeMirror typeMirror) {
        return TypeFactory.select(TypeFactory.of(processingEnv.getTypeUtils(), asTypeElement(typeMirror)));
    }

    private TypeElement asTypeElement(String o) {
        return processingEnv.getElementUtils().getTypeElement(o);
    }

    private TypeElement asTypeElement(Class o) {
        return processingEnv.getElementUtils().getTypeElement(o.getName());
    }

    private TypeElement asTypeElement(ClassName o) {
        return processingEnv.getElementUtils().getTypeElement(o.toString());
    }

    private TypeElement asTypeElement(TypeMirror o) {
        return (TypeElement) processingEnv.getTypeUtils().asElement(o);
    }

    private void error(TypeElement annotatedElement, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, annotatedElement);
    }

    private void warn(TypeElement annotatedElement, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message, annotatedElement);
    }

    static ClassName resolveLoaderName(ServiceLoaderGenerator generator) {
        return resolveLoaderName(generator.getLoaderName(), generator.getServiceType());
    }

    static ClassName resolveLoaderName(String loaderName, ClassName serviceType) {
        if (!loaderName.isEmpty()) {
            return ClassName.bestGuess(loaderName);
        }
        ClassName top = serviceType.topLevelClassName();
        ClassName topLoader = ClassName.get(top.packageName(), top.simpleName() + "Loader");
        if (top.equals(serviceType)) {
            return topLoader;
        }
        return topLoader.nestedClass(serviceType.simpleName());
    }
}
