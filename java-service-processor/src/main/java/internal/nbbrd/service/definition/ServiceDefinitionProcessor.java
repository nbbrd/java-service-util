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
import nbbrd.service.Quantifier;

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
                .map(definitionType -> ServiceLoaderGenerator.of(definitionType, processingEnv.getTypeUtils()))
                .collect(Collectors.toList());
    }

    private boolean check(ServiceLoaderGenerator generator) {
        Types types = processingEnv.getTypeUtils();
        TypeElement service = asTypeElement(generator.getServiceType());

        if (!checkFallback(generator.getQuantifier(), generator.getFallback(), service, types)) {
            return false;
        }
        if (!checkPreprocessor(generator.getPreprocessor(), service, types)) {
            return false;
        }
        if (!checkMutability(generator, service, types)) {
            return false;
        }
        return true;
    }

    private boolean checkFallback(Quantifier quantifier, Fallback fallback, TypeElement service, Types types) {
        switch (quantifier) {
            case SINGLE:
                if (!fallback.getTypeHandler().isPresent()) {
                    warn(service, String.format("Missing fallback for service '%1$s'", service));
                }
                break;
            case MULTIPLE:
            case OPTIONAL:
                if (fallback.getTypeHandler().isPresent()) {
                    warn(service, String.format("Useless fallback for service '%1$s'", service));
                }
                break;
        }

        if (fallback.getTypeHandler().isPresent()) {
            if (!checkFallbackTypeHandler(fallback.getTypeHandler().get(), service, types)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkFallbackTypeHandler(TypeHandler handler, TypeElement service, Types types) {
        if (!types.isAssignable(handler.getType(), types.erasure(service.asType()))) {
            error(service, String.format("Fallback '%1$s' doesn't extend nor implement service '%2$s'", handler.getType(), service));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkPreprocessor(Preprocessor preprocessor, TypeElement service, Types types) {
        if (preprocessor.getTypeHandler().isPresent()) {
            if (!checkPreprocessorTypeHandler(preprocessor.getTypeHandler().get(), service, types)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPreprocessorTypeHandler(TypeHandler handler, TypeElement service, Types types) {
        DeclaredType streamOf = types.getDeclaredType(asTypeElement(Stream.class), service.asType());
        DeclaredType unaryOperatorOf = types.getDeclaredType(asTypeElement(UnaryOperator.class), streamOf);
        if (!types.isAssignable(handler.getType(), unaryOperatorOf)) {
            error(service, String.format("Preprocessor '%1$s' doesn't extend nor implement 'UnaryOperator<Stream<%2$s>>'", handler.getType(), service));
            return false;
        }

        if (!checkInstanceFactories(service, handler.getType(), handler)) {
            return false;
        }
        return true;
    }

    private boolean checkMutability(ServiceLoaderGenerator generator, TypeElement service, Types types) {
        if (generator.getLifecycle() == Lifecycle.UNSAFE_MUTABLE) {
            warn(service, String.format("Thread-unsafe singleton for '%1$s'", service));
        }
        return true;
    }

    private boolean checkInstanceFactories(TypeElement annotatedElement, TypeMirror type, TypeHandler instance) {
        if (!instance.select().isPresent()) {
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
        JavaFile file = isNotNested(top, generators)
                ? generate(top, generators.get(0))
                : generateNested(top, generators);
        ProcessorUtil.write(processingEnv, file);
    }

    private boolean isNotNested(ClassName top, List<ServiceLoaderGenerator> generators) {
        return generators.size() == 1 && top.equals(resolveLoaderName(generators.get(0)));
    }

    private JavaFile generate(ClassName top, ServiceLoaderGenerator generator) {
        return JavaFile.builder(top.packageName(), generator.generate(top.simpleName())).build();
    }

    private JavaFile generateNested(ClassName top, List<ServiceLoaderGenerator> generators) {
        TypeSpec.Builder nestedTypes = TypeSpec.classBuilder(top)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        generators
                .stream()
                .map(generator -> generator.generate(resolveLoaderName(generator).simpleName()))
                .map(o -> o.toBuilder().addModifiers(Modifier.STATIC).build())
                .forEach(nestedTypes::addType);
        return JavaFile.builder(top.packageName(), nestedTypes.build()).build();
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
