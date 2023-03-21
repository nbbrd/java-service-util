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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Philippe Charles
 */
@org.openide.util.lookup.ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes({
        "nbbrd.service.ServiceProvider",
        "nbbrd.service.ServiceProvider.List"
})
public final class ServiceProviderProcessor extends AbstractProcessor {

    private final ServiceProviderCollector collector = new ServiceProviderCollector(() -> processingEnv);

    private final ServiceProviderChecker checker = new ServiceProviderChecker(() -> processingEnv);

    private final ServiceProviderGenerator generator = new ServiceProviderGenerator(() -> processingEnv);

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        collector.collect(annotations, roundEnv);

        if (roundEnv.processingOver()) {
            List<ProviderRef> providers = collector.build();
            try {
                if (checker.check(providers)) {
                    generator.generate(providers);
                }
            } catch (IOException ex) {
                reportUnexpectedError(ex);
            }
            collector.clear();
        }

        return false;
    }

    private void reportUnexpectedError(IOException ex) {
        String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }
}
