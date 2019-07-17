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
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

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

    private static <T> List<T> concat(List<T> first, List<T> second) {
        List<T> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }
}
