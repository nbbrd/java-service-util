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

import internal.nbbrd.service.ModuleInfoEntries;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
final class ModulePathRegistry implements ProviderRegistry {

    @lombok.NonNull
    private final ProcessingEnvironment env;

    public Optional<List<ProviderEntry>> readAll() throws IOException {
        return ModuleInfoEntries.parse(env.getFiler(), env.getElementUtils())
                .map(ModulePathRegistry::parseAll);
    }

    static List<ProviderEntry> parseAll(ModuleInfoEntries content) {
        return content
                .getProvisions()
                .entrySet()
                .stream()
                .flatMap(ModulePathRegistry::parse)
                .collect(Collectors.toList());
    }

    private static Stream<ProviderEntry> parse(Map.Entry<String, List<String>> entry) {
        return entry.getValue()
                .stream()
                .map(provider -> new ProviderEntry(entry.getKey(), provider));
    }
}
