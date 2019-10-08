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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
final class ClassPathRegistry implements ProviderRegistry {

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
        Elements util = env.getElementUtils();
        return refs
                .stream()
                .filter(ref -> ref.getService().equals(service))
                .map(ProviderRef::getProvider)
                .map(provider -> util.getBinaryName(util.getTypeElement(provider)))
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

    private String getRelativeName(Name service) {
        Elements util = env.getElementUtils();
        return "META-INF/services/" + util.getBinaryName(util.getTypeElement(service));
    }
}
