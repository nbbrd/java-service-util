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

import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
final class ClassPathRegistry implements ProviderRegistry {

    @lombok.NonNull
    private final ProcessingEnvironment env;

    public List<ProviderConfigurationFileLine> readLinesByService(TypeElement service) throws IOException {
        FileObject src = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", getFileRelativeName(service));
        try (BufferedReader reader = new BufferedReader(src.openReader(false))) {
            return readLinesByService(reader, service, src.toUri());
        } catch (FileNotFoundException | NoSuchFileException | FilerException ex) {
            // ignore
            return Collections.emptyList();
        }
    }

    private static List<ProviderConfigurationFileLine> readLinesByService(BufferedReader reader, TypeElement service, URI uri) throws IOException {
        List<ProviderConfigurationFileLine> result = new ArrayList<>();
        int lineNumber = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                result.add(ProviderConfigurationFileLine.parse(line));
            } catch (IllegalArgumentException ex) {
                throw new IOException(service.getQualifiedName() + ": " + (uri + ":" + lineNumber + ": " + ex.getMessage()));
            }
            lineNumber++;
        }
        return result;
    }

    public void writeLinesByService(List<ProviderConfigurationFileLine> lines, TypeElement service) throws IOException {
        FileObject dst = env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", getFileRelativeName(service));
        try (BufferedWriter writer = new BufferedWriter(dst.openWriter())) {
            for (ProviderConfigurationFileLine line : lines) {
                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    public List<ProviderEntry> parseAll(TypeElement service, List<ProviderConfigurationFileLine> lines) {
        String serviceName = service.getQualifiedName().toString();
        return lines
                .stream()
                .map(ProviderConfigurationFileLine::getProviderBinaryName)
                .filter(Objects::nonNull)
                .map(providerName -> new ProviderEntry(serviceName, providerName))
                .collect(Collectors.toList());
    }

    public List<ProviderConfigurationFileLine> formatAll(TypeElement service, List<ProviderRef> refs) {
        Elements util = env.getElementUtils();
        return refs
                .stream()
                .filter(ref -> ref.getService().equals(service))
                .map(ref -> util.getBinaryName(ref.getProvider()))
                .map(ProviderConfigurationFileLine::ofProviderBinaryName)
                .collect(Collectors.toList());
    }

    private String getFileRelativeName(TypeElement service) {
        return ProviderConfigurationFileLine.getFileRelativeName(env.getElementUtils().getBinaryName(service));
    }
}
