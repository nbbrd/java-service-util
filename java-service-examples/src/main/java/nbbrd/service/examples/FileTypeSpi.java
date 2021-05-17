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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;
import nbbrd.service.ServiceSorter;

/**
 *
 * @author Philippe Charles
 */
@ServiceDefinition(
        quantifier = Quantifier.MULTIPLE,
        loaderName = "internal.FileTypeSpiLoader")
public interface FileTypeSpi {

    enum Accuracy {
        HIGH, LOW
    }

    String getContentTypeOrNull(Path file) throws IOException;

    @ServiceSorter
    Accuracy getAccuracy();

    @ServiceProvider
    final class ByExtensionProbe implements FileTypeSpi {

        final Map<String, String> typeByExtension;

        public ByExtensionProbe() {
            this.typeByExtension = new HashMap<>();
            typeByExtension.put("csv", "text/csv");
            typeByExtension.put("avi", "video/x-msvideo");
        }

        @Override
        public String getContentTypeOrNull(Path file) throws IOException {
            return typeByExtension.get(getExtension(file));
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
    final class ByMagicNumberProbe implements FileTypeSpi {

        @Override
        public String getContentTypeOrNull(Path file) throws IOException {
            String magicId = getMagicIdAsString(file, 8);
            if (magicId.startsWith(XLS_HEADER)) {
                return "application/vnd.ms-excel";
            }
            if (magicId.startsWith(PDF_HEADER)) {
                return "application/pdf";
            }
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
                    return IntStream.range(0, n).mapToObj(i -> String.format("%02x", bytes[i])).collect(Collectors.joining()).toUpperCase();
                }
            }
            return "";
        }

        private static final String XLS_HEADER = "D0CF11E0A1B11AE1";
        private static final String PDF_HEADER = "255044462D";
    }
}
