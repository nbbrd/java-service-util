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
package internal.nbbrd.service.provider;

import internal.nbbrd.service.Instantiator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Philippe Charles
 */
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes({"nbbrd.service.ServiceProvider", "nbbrd.service.ServiceProvider.List"})
public final class ServiceProviderProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

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

        if (types.isSameType(ref.getService().asType(), elements.getTypeElement(Void.class.getName()).asType())) {
            error(ref, "Cannot infer service from provider " + ref.getProvider());
            return;
        }

        if (!types.isAssignable(ref.getProvider().asType(), types.erasure(ref.getService().asType()))) {
            error(ref, String.format("Provider '%1$s' doesn't extend nor implement service '%2$s'", ref.getProvider(), ref.getService()));
            return;
        }

        if (ref.getProvider().getEnclosingElement().getKind() == ElementKind.CLASS && !ref.getProvider().getModifiers().contains(Modifier.STATIC)) {
            error(ref, String.format("Provider '%1$s' must be static inner class", ref.getProvider()));
            return;
        }

        if (ref.getProvider().getModifiers().contains(Modifier.ABSTRACT)) {
            error(ref, String.format("Provider '%1$s' must not be abstract", ref.getProvider()));
            return;
        }

        if (Instantiator.allOf(types, ref.getService(), ref.getProvider()).stream().noneMatch(this::isValidInstantiator)) {
            error(ref, String.format("Provider '%1$s' must have a public no-argument constructor", ref.getProvider()));
            return;
        }
    }

    private boolean isValidInstantiator(Instantiator instantiator) {
        switch (instantiator.getKind()) {
            case CONSTRUCTOR:
                return true;
            case STATIC_METHOD:
                return instantiator.getElement().getSimpleName().contentEquals("provider");
            default:
                return false;
        }
    }

    private void log(String id, List<ProviderRef> refs) {
        refs.forEach(ref -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("%1$s: %2$s", id, ref)));
    }

    private void logEntries(String id, List<ProviderEntry> entries) {
        entries.forEach(entry -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("%1$s: %2$s", id, entry)));
    }

    private void error(ProviderRef ref, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, ref.getProvider());
    }

    private void errorEntry(ProviderEntry entry, String message) {
        TypeElement optionalType = processingEnv.getElementUtils().getTypeElement(entry.getProvider());
        if (optionalType != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, optionalType);
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
        }
    }

    private void checkModulePath(List<ProviderRef> annotationRefs, ModulePathRegistry modulePath) throws IOException {
        modulePath.readAll().ifPresent(modulePathEntries -> {
            logEntries("ModulePath", modulePathEntries);

            getMissingRefs(annotationRefs, modulePathEntries)
                    .forEachOrdered(ref -> error(ref, "Missing module-info.java 'provides' directive for '" + ref + "'"));

            System.out.println(Arrays.toString(annotationRefs.toArray()));
            System.out.println(Arrays.toString(modulePathEntries.toArray()));

            getMissingEntries(annotationRefs, modulePathEntries)
                    .forEachOrdered(entry -> errorEntry(entry, "Missing annotation for '" + entry + "'"));
        });
    }

    static Stream<ProviderRef> getMissingRefs(List<ProviderRef> annotationRefs, List<ProviderEntry> modulePathEntries) {
        return annotationRefs
                .stream()
                .filter(ref -> !modulePathEntries.contains(ref.toEntry()));
    }

    static Stream<ProviderEntry> getMissingEntries(List<ProviderRef> annotationRefs, List<ProviderEntry> modulePathEntries) {
        Set<ProviderEntry> annotationEntries = annotationRefs.stream().map(ProviderRef::toEntry).collect(Collectors.toSet());

        return modulePathEntries
                .stream()
                .filter(entry -> (!annotationEntries.contains(entry)));
    }

    private void registerClassPath(List<ProviderRef> annotationRefs, ClassPathRegistry classPath) throws IOException {
        Map<TypeElement, List<ProviderRef>> refByService = annotationRefs.stream().collect(Collectors.groupingBy(ProviderRef::getService));

        for (Map.Entry<TypeElement, List<ProviderRef>> x : refByService.entrySet()) {
            List<String> oldLines = classPath.readLinesByService(x.getKey());
            logEntries("ClassPath", classPath.parseAll(x.getKey(), oldLines));

            List<String> newLines = classPath.formatAll(x.getKey(), generateDelegates(x.getValue()));
            classPath.writeLinesByService(merge(oldLines, newLines), x.getKey());
        }
    }

    private List<ProviderRef> generateDelegates(List<ProviderRef> refs) {
        for (ProviderRef ref : refs) {
            if (Instantiator.allOf(processingEnv.getTypeUtils(), ref.getService(), ref.getProvider()).stream().anyMatch(o -> o.getKind() == Instantiator.Kind.STATIC_METHOD)) {
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
}
