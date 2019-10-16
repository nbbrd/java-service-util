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
import java.util.List;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;
import nbbrd.service.ServiceSorter;

/**
 *
 * @author Philippe Charles
 */
@ServiceDefinition
public interface FileSearch {

    List<File> searchByName(String name);

    @ServiceFilter
    boolean isAvailable();

    @ServiceSorter
    int getCost();

    public static void main(String[] args) {
        FileSearchLoader.load().ifPresent(search -> search.searchByName(".xlsx").forEach(System.out::println));
    }
}
