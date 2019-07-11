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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import nbbrd.service.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@lombok.extern.java.Log
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("nbbrd.service.ServiceProvider")
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

        annotationRefs.forEach(this::checkRef);

        try {
            checkModulePath(annotationRefs, new ModulePathRegistry(processingEnv));
            registerClassPath(annotationRefs, new ClassPathRegistry(processingEnv));
        } catch (IOException ex) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }

        return true;
    }

    private void checkRef(ProviderRef ref) {
        TypeElement service = processingEnv.getElementUtils().getTypeElement(ref.getService());
        if (service == null) {
            error(ref, "Cannot find service " + ref.getService());
            return;
        }

        TypeElement provider = processingEnv.getElementUtils().getTypeElement(ref.getProvider());
        if (provider == null) {
            error(ref, "Cannot find provider " + ref.getProvider());
            return;
        }

        if (!processingEnv.getTypeUtils().isSubtype(provider.asType(), service.asType())) {
            error(ref, "Provider " + ref.getProvider() + " doesn't extend nor implement service " + ref.getService());
        }
    }

    private void log(String id, List<ProviderRef> refs) {
        refs.forEach(ref -> log.log(Level.INFO, "{0}: {1}", new Object[]{id, ref}));
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
            List<String> newLines = classPath.formatAll(x.getKey(), x.getValue());

            log("ClassPath", classPath.parseAll(x.getKey(), oldLines));

            classPath.writeLinesByService(concat(oldLines, newLines), x.getKey());
        }
    }

    @lombok.Value
    static class ProviderRef {

        @lombok.NonNull
        private Name service;

        @lombok.NonNull
        private Name provider;

        @Override
        public String toString() {
            return service + "/" + provider;
        }
    }

    interface ProviderRegistry {

    }

    @lombok.RequiredArgsConstructor
    static final class AnnotationRegistry implements ProviderRegistry {

        @lombok.NonNull
        private final Set<? extends TypeElement> annotations;

        @lombok.NonNull
        private final RoundEnvironment roundEnv;

        public List<ProviderRef> readAll() {
            return annotations.stream()
                    .map(roundEnv::getElementsAnnotatedWith)
                    .flatMap(Set::stream)
                    .map(TypeElement.class::cast)
                    .map(AnnotationRegistry::newRef)
                    .collect(Collectors.toList());
        }

        static ProviderRef newRef(TypeElement type) {
            TypeMirror serviceType = extractResultType(type.getAnnotation(ServiceProvider.class)::value);
            Name serviceName = ((TypeElement) ((DeclaredType) serviceType).asElement()).getQualifiedName();
            Name providerName = type.getQualifiedName();
            return new ProviderRef(serviceName, providerName);
        }

        // see http://hauchee.blogspot.be/2015/12/compile-time-annotation-processing-getting-class-value.html
        static TypeMirror extractResultType(Supplier<Class<?>> type) {
            try {
                type.get();
                throw new RuntimeException("Expecting exeption to be raised");
            } catch (MirroredTypeException ex) {
                return ex.getTypeMirror();
            }
        }
    }

    @lombok.RequiredArgsConstructor
    static final class ClassPathRegistry implements ProviderRegistry {

        @lombok.NonNull
        private final ProcessingEnvironment env;

        public List<String> readLinesByService(Name service) throws IOException {
            FileObject src = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", getRelativeName(service));
            try (BufferedReader reader = new BufferedReader(src.openReader(false))) {
                List<String> result = new ArrayList<>();
                String rawProvider;
                while ((rawProvider = reader.readLine()) != null) {
                    result.add(rawProvider);
                }
                return result;
            } catch (FileNotFoundException | NoSuchFileException | FilerException ex) {
                // ignore
                return Collections.emptyList();
            }
        }

        public void writeLinesByService(List<String> lines, Name service) throws IOException {
            FileObject dst = env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", getRelativeName(service));
            try (BufferedWriter writer = new BufferedWriter(dst.openWriter())) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }

        public List<ProviderRef> parseAll(Name service, List<String> lines) {
            return lines
                    .stream()
                    .map(env.getElementUtils()::getName)
                    .map(name -> new ProviderRef(service, name))
                    .collect(Collectors.toList());
        }

        public List<String> formatAll(Name service, List<ProviderRef> refs) {
            return refs
                    .stream()
                    .filter(ref -> ref.getService().equals(service))
                    .map(ProviderRef::getProvider)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        static ProviderRef parse(Function<String, Name> nameFactory, Name service, String rawProvider) {
            int commentIndex = rawProvider.indexOf('#');
            if (commentIndex != -1) {
                rawProvider = rawProvider.substring(0, commentIndex);
            }
            rawProvider = rawProvider.trim();
            return new ProviderRef(service, nameFactory.apply(rawProvider));
        }

        static String getRelativeName(Name service) {
            return "META-INF/services/" + service.toString();
        }
    }

    @lombok.RequiredArgsConstructor
    static final class ModulePathRegistry implements ProviderRegistry {

        @lombok.NonNull
        private final ProcessingEnvironment env;

        public Optional<List<ProviderRef>> readAll() throws IOException {
            try {
                FileObject src = env.getFiler().getResource(StandardLocation.SOURCE_PATH, "", "module-info.java");
                return Optional.of(parseAll(env.getElementUtils()::getName, src.getCharContent(false)));
            } catch (FileNotFoundException | NoSuchFileException | FilerException ex) {
                // ignore
                return Optional.empty();
            }
        }

        static final String CLASS_PATTERN = "[a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)+[0-9a-z_]";
        static final String PROVIDES_PATTERN = "provides\\s+(" + CLASS_PATTERN + ")\\s+with\\s+(" + CLASS_PATTERN + "(?:\\s*,\\s*" + CLASS_PATTERN + ")*)";

        static List<ProviderRef> parseAll(Function<String, Name> nameFactory, CharSequence content) {
            Pattern p = Pattern.compile(PROVIDES_PATTERN, Pattern.CASE_INSENSITIVE);

            List<ProviderRef> result = new ArrayList<>();
            Matcher m = p.matcher(content);
            while (m.find()) {
                Name service = nameFactory.apply(m.group(1));
                for (String rawProvider : m.group(2).split(",", -1)) {
                    Name provider = nameFactory.apply(rawProvider.trim());
                    result.add(new ProviderRef(service, provider));
                }
            }
            return result;
        }
    }

    private static <T> List<T> concat(List<T> first, List<T> second) {
        List<T> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }
}
