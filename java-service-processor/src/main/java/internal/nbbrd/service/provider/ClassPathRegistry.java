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
import java.util.stream.Collectors;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
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

    public List<String> readLinesByService(TypeElement service) throws IOException {
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

    public void writeLinesByService(List<String> lines, TypeElement service) throws IOException {
        FileObject dst = env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", getRelativeName(service));
        try (BufferedWriter writer = new BufferedWriter(dst.openWriter())) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    public List<ProviderEntry> parseAll(TypeElement service, List<String> lines) {
        String serviceName = service.getQualifiedName().toString();
        return lines
                .stream()
                .map(line -> parse(serviceName, line))
                .collect(Collectors.toList());
    }

    public List<String> formatAll(TypeElement service, List<ProviderRef> refs) {
        Elements util = env.getElementUtils();
        return refs
                .stream()
                .filter(ref -> ref.getService().equals(service))
                .map(ref -> util.getBinaryName(ref.getProvider()).toString())
                .collect(Collectors.toList());
    }

    static ProviderEntry parse(String service, String line) {
        int commentIndex = line.indexOf('#');
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        line = line.trim();
        return new ProviderEntry(service, line);
    }

    private String getRelativeName(TypeElement service) {
        return "META-INF/services/" + env.getElementUtils().getBinaryName(service);
    }
}
