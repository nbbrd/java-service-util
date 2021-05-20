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

import java.util.Optional;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
@ServiceDefinition(quantifier = Quantifier.OPTIONAL)
public interface WinRegistry {

    String readString(int hkey, String key, String valueName);

    int HKEY_LOCAL_MACHINE = 0;

    static void main(String[] args) {
        Optional<WinRegistry> optional = WinRegistryLoader.load();
        optional.ifPresent(reg -> System.out.println(reg.readString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName")));
    }
}
