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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Philippe Charles
 */
public final class FileType {

    private FileType() {
        // static class
    }

    private static final List<FileTypeSpi> PROBES = internal.FileTypeSpiLoader.load();

    public static Optional<String> probeContentType(Path file) throws IOException {
        for (FileTypeSpi probe : PROBES) {
            String result;
            if ((result = probe.getContentTypeOrNull(file)) != null) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    public static void main(String[] args) throws IOException {
        String[] files = {"hello.csv", "stuff.txt"};
        for (String file : files) {
            System.out.println(file + ": " + FileType.probeContentType(Paths.get(file)).orElse("?"));
        }
    }
}
