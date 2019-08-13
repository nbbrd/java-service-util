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
package internal.nbbrd.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 *
 * @author Philippe Charles
 */
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"nbbrd.service.ServiceProvider", "nbbrd.service.ServiceProvider.List"})
public final class ServiceProviderProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        List<ProviderRef> annotationRefs = new AnnotationRegistry(annotations, roundEnv).readAll();

        if (annotationRefs.isEmpty()) {
            return true;
        }

        log("Annotation", annotationRefs);

        checkRefs(annotationRefs);

        try {
            checkModulePath(annotationRefs, new ModulePathRegistry(processingEnv));
            registerClassPath(annotationRefs, new ClassPathRegistry(processingEnv));
        } catch (IOException ex) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }

        return true;
    }

    private void checkRefs(List<ProviderRef> refs) {
        ProviderRef.getDuplicates(refs)
                .forEach(ref -> error(ref, String.format("Duplicated provider: '%1$s'", ref.getProvider())));

        refs.forEach(this::checkRef);
    }

    private void checkRef(ProviderRef ref) {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();

        TypeElement service = elements.getTypeElement(ref.getService());
        if (service == null) {
            error(ref, "Cannot find service " + ref.getService());
            return;
        }

        TypeElement provider = elements.getTypeElement(ref.getProvider());
        if (provider == null) {
            error(ref, "Cannot find provider " + ref.getProvider());
            return;
        }

        if (!types.isAssignable(provider.asType(), types.erasure(service.asType()))) {
            error(ref, String.format("Provider '%1$s' doesn't extend nor implement service '%2$s'", ref.getProvider(), ref.getService()));
            return;
        }

        if (provider.getEnclosingElement().getKind() == ElementKind.CLASS && !provider.getModifiers().contains(Modifier.STATIC)) {
            error(ref, String.format("Provider '%1$s' must be static inner class", ref.getProvider()));
            return;
        }

        if (provider.getModifiers().contains(Modifier.ABSTRACT)) {
            error(ref, String.format("Provider '%1$s' must not be abstract", ref.getProvider()));
            return;
        }

        if (FactoryType.of(types, service, provider) == FactoryType.NONE) {
            error(ref, String.format("Provider '%1$s' must have a public no-argument constructor", ref.getProvider()));
            return;
        }
    }

    private void log(String id, List<ProviderRef> refs) {
        refs.forEach(ref -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("%1$s: %2$s", id, ref)));
    }

    private void error(ProviderRef ref, String message) {
        TypeElement provider = processingEnv.getElementUtils().getTypeElement(ref.getProvider());
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, provider);
    }

    private void checkModulePath(List<ProviderRef> annotationRefs, ModulePathRegistry modulePath) throws IOException {
        modulePath.readAll().ifPresent(modulePathRefs -> {
            log("ModulePath", modulePathRefs);

            annotationRefs
                    .stream()
                    .filter(ref -> (!modulePathRefs.contains(ref)))
                    .forEachOrdered(ref -> error(ref, "Missing module-info.java entry for '" + ref + "'"));

            modulePathRefs
                    .stream()
                    .filter(ref -> (!annotationRefs.contains(ref)))
                    .forEachOrdered(ref -> error(ref, "Missing annotation for '" + ref + "'"));
        });
    }

    private void registerClassPath(List<ProviderRef> annotationRefs, ClassPathRegistry classPath) throws IOException {
        Map<Name, List<ProviderRef>> refByService = annotationRefs.stream().collect(Collectors.groupingBy(ProviderRef::getService));

        for (Map.Entry<Name, List<ProviderRef>> x : refByService.entrySet()) {
            List<String> oldLines = classPath.readLinesByService(x.getKey());
            log("ClassPath", classPath.parseAll(x.getKey(), oldLines));

            List<String> newLines = classPath.formatAll(x.getKey(), generateDelegates(x.getValue()));
            classPath.writeLinesByService(merge(oldLines, newLines), x.getKey());
        }
    }

    private List<ProviderRef> generateDelegates(List<ProviderRef> refs) {
        for (ProviderRef ref : refs) {
            TypeElement service = processingEnv.getElementUtils().getTypeElement(ref.getService());
            TypeElement provider = processingEnv.getElementUtils().getTypeElement(ref.getProvider());
            if (FactoryType.of(processingEnv.getTypeUtils(), service, provider) == FactoryType.STATIC_METHOD) {
                error(ref, "Static method support not implemented yet");
            }
        }
        return refs;
    }

    static List<String> merge(List<String> first, List<String> second) {
        List<String> result = new ArrayList<>(first);
        second.stream()
                .filter(element -> !result.contains(element))
                .forEach(result::add);
        return result;
    }

    private static boolean hasPublicNoArgumentConstructor(TypeElement type) {
        return ElementFilter
                .constructorsIn(type.getEnclosedElements())
                .stream()
                .anyMatch(ServiceProviderProcessor::isNoArgPublicMethod);
    }

    private static boolean isNoArgPublicMethod(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.PUBLIC) && method.getParameters().isEmpty();
    }

    private static Optional<ExecutableElement> getStaticFactoryMethod(Types types, TypeElement service, TypeElement provider) {
        return ElementFilter
                .methodsIn(provider.getEnclosedElements())
                .stream()
                .filter(ServiceProviderProcessor::isNoArgPublicMethod)
                .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> types.isSubtype(method.getReturnType(), service.asType()))
                .findFirst();
    }

    private enum FactoryType {
        NONE, CONSTRUCTOR, STATIC_METHOD;

        static FactoryType of(Types types, TypeElement service, TypeElement provider) {
            if (hasPublicNoArgumentConstructor(provider)) {
                return CONSTRUCTOR;
            }
            if (getStaticFactoryMethod(types, service, provider).isPresent()) {
                return STATIC_METHOD;
            }
            return NONE;
        }
    }
}
