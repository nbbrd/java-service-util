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

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
@ServiceDefinition(preprocessor = FileSearch.ByAvailabilityAndCost.class, singleton = true)
public interface FileSearch {

    List<File> searchByName(String name);

    boolean isAvailable();

    int getCost();

    public static class ByAvailabilityAndCost implements UnaryOperator<Stream<FileSearch>> {

        @Override
        public Stream<FileSearch> apply(Stream<FileSearch> stream) {
            return stream
                    .filter(FileSearch::isAvailable)
                    .sorted(Comparator.comparing(FileSearch::getCost));
        }
    }

    public static void main(String[] args) {
        FileSearchLoader.get().ifPresent(search -> search.searchByName(".xlsx").forEach(System.out::println));
    }
}
