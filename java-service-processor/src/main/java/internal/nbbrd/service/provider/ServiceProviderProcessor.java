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
import javax.lang.model.element.Name;
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
@SupportedAnnotationTypes({
        "nbbrd.service.ServiceProvider",
        "nbbrd.service.ServiceProvider.List"
})
public final class ServiceProviderProcessor extends AbstractProcessor {

    // we store Name instead of TypeElement due to a bug(?) in JDK8
    // that creates not-equivalent elements between rounds
    private final Set<Name> pendingRefs = new HashSet<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        pushPending(new AnnotationRegistry(annotations, roundEnv).readAll());

        if (roundEnv.processingOver()) {
            List<ProviderRef> annotationRefs = popPending();

            try {
                checkRefs(annotationRefs);
                writeRefs(annotationRefs);
            } catch (RefError ex) {
                reportError(ex.ref, ex.getMessage());
            }
        }

        return false;
    }

    private void pushPending(List<ProviderRef> refs) {
        refs.forEach(ref -> pendingRefs.add(ref.getProvider().getQualifiedName()));
    }

    private List<ProviderRef> popPending() {
        List<ProviderRef> result = pendingRefs
                .stream()
                .map(x -> processingEnv.getElementUtils().getTypeElement(x.toString()))
                .flatMap(AnnotationRegistry::newRefs)
                .collect(Collectors.toList());
        pendingRefs.clear();
        return result;
    }

    private void checkRefs(List<ProviderRef> refs) {
        ProviderRef.getDuplicates(refs)
                .forEach(ref -> {
                    throw new RefError(ref, String.format("Duplicated provider: '%1$s'", ref.getProvider()));
                });

        refs.forEach(this::checkRef);
    }

    private void writeRefs(List<ProviderRef> annotationRefs) {
        log("Annotation", annotationRefs);
        try {
            checkModulePath(annotationRefs, new ModulePathRegistry(processingEnv));
            registerClassPath(annotationRefs, new ClassPathRegistry(processingEnv));
        } catch (IOException ex) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
    }

    private void checkRef(ProviderRef ref) {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();

        if (types.isSameType(ref.getService().asType(), elements.getTypeElement(Void.class.getName()).asType())) {
            throw new RefError(ref, "Cannot infer service from provider " + ref.getProvider());
        }

        if (!types.isAssignable(ref.getProvider().asType(), types.erasure(ref.getService().asType()))) {
            throw new RefError(ref, String.format("Provider '%1$s' doesn't extend nor implement service '%2$s'", ref.getProvider(), ref.getService()));
        }

        if (ref.getProvider().getEnclosingElement().getKind() == ElementKind.CLASS && !ref.getProvider().getModifiers().contains(Modifier.STATIC)) {
            throw new RefError(ref, String.format("Provider '%1$s' must be static inner class", ref.getProvider()));
        }

        if (ref.getProvider().getModifiers().contains(Modifier.ABSTRACT)) {
            throw new RefError(ref, String.format("Provider '%1$s' must not be abstract", ref.getProvider()));
        }

        if (Instantiator.allOf(types, ref.getService(), ref.getProvider()).stream().noneMatch(this::isValidInstantiator)) {
            throw new RefError(ref, String.format("Provider '%1$s' must have a public no-argument constructor", ref.getProvider()));
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

    private void reportError(ProviderRef ref, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, ref.getProvider());
    }

    private void log(String id, List<ProviderRef> refs) {
        refs.forEach(ref -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("%1$s: %2$s", id, ref)));
    }

    private void logEntries(String id, List<ProviderEntry> entries) {
        entries.forEach(entry -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("%1$s: %2$s", id, entry)));
    }

    static class RefError extends IllegalArgumentException {

        final ProviderRef ref;

        RefError(ProviderRef ref, String message) {
            super(message);
            this.ref = ref;
        }
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
                    .forEachOrdered(ref -> {
                        throw new RefError(ref, "Missing module-info directive 'provides " + ref.getService() + " with " + ref.getProvider() + "'");
                    });

//            System.out.println(Arrays.toString(annotationRefs.toArray()));
//            System.out.println(Arrays.toString(modulePathEntries.toArray()));

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
                reportError(ref, "Static method support not implemented yet");
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
