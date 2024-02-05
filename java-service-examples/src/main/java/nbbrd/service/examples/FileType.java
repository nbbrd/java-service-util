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
package nbbrd.service.examples;

import internal.nbbrd.service.examples.FileTypeSpiLoader;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;
import nbbrd.service.ServiceSorter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Philippe Charles
 */
public final class FileType {

    private FileType() {
        // static class
    }

    public static Optional<String> probeContentType(Path file) throws IOException {
        for (FileTypeSpi probe : FileTypeSpiLoader.get()) {
            String result = probe.getContentTypeOrNull(file);
            if (result != null) return Optional.of(result);
        }
        return Optional.empty();
    }

    public static void main(String[] args) throws IOException {
        String[] files = {"hello.csv", "stuff.txt"};
        for (String file : files) {
            System.out.println(file + ": " + FileType.probeContentType(Paths.get(file)).orElse("?"));
        }
    }

    @ServiceDefinition(
            quantifier = Quantifier.MULTIPLE,
            loaderName = "internal.{{canonicalName}}Loader",
            singleton = true
    )
    public interface FileTypeSpi {

        enum Accuracy {HIGH, LOW}

        String getContentTypeOrNull(Path file) throws IOException;

        @ServiceSorter
        Accuracy getAccuracy();
    }

    @ServiceProvider
    public static final class ByExtensionProbe implements FileTypeSpi {

        @Override
        public String getContentTypeOrNull(Path file) {
            switch (getExtension(file)) {
                case "csv":
                    return "text/csv";
                case "avi":
                    return "video/x-msvideo";
                default:
                    return null;
            }
        }

        @Override
        public Accuracy getAccuracy() {
            return Accuracy.LOW;
        }

        private String getExtension(Path file) {
            int index = file.toString().lastIndexOf('.');
            return index != -1 ? file.toString().substring(index + 1).toLowerCase(Locale.ROOT) : "";
        }
    }

    @ServiceProvider
    public static final class ByMagicNumberProbe implements FileTypeSpi {

        @Override
        public String getContentTypeOrNull(Path file) throws IOException {
            String magicId = getMagicIdAsString(file, 8);
            if (magicId.startsWith("D0CF11E0A1B11AE1")) return "application/vnd.ms-excel";
            if (magicId.startsWith("255044462D")) return "application/pdf";
            return null;
        }

        @Override
        public Accuracy getAccuracy() {
            return Accuracy.HIGH;
        }

        private String getMagicIdAsString(Path file, int maxBytes) throws IOException {
            if (Files.isReadable(file)) {
                try (InputStream stream = Files.newInputStream(file)) {
                    byte[] bytes = new byte[maxBytes];
                    int n = stream.read(bytes);
                    return IntStream.range(0, n)
                            .mapToObj(i -> String.format(Locale.ROOT, "%02x", bytes[i]))
                            .collect(Collectors.joining())
                            .toUpperCase(Locale.ROOT);
                }
            }
            return "";
        }
    }
}
