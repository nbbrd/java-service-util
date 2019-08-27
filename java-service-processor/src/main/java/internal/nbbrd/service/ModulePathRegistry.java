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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
final class ModulePathRegistry implements ProviderRegistry {

    @lombok.NonNull
    private final ProcessingEnvironment env;

    public Optional<List<ProviderRef>> readAll() throws IOException {
        Function<String, Name> nameFactory = env.getElementUtils()::getName;
        return ModuleInfoEntries.parse(env.getFiler())
                .map(entries -> parseAll(nameFactory, entries));
    }

    static List<ProviderRef> parseAll(Function<String, Name> nameFactory, ModuleInfoEntries content) {
        return content
                .getProvisions()
                .entrySet()
                .stream()
                .flatMap(entry -> parse(nameFactory, entry))
                .collect(Collectors.toList());
    }

    private static Stream<ProviderRef> parse(Function<String, Name> nameFactory, Map.Entry<String, List<String>> entry) {
        Name service = nameFactory.apply(entry.getKey());
        return entry.getValue()
                .stream()
                .map(nameFactory)
                .map(provider -> new ProviderRef(service, provider));
    }
}
