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

import static internal.nbbrd.service.CustomName.newRef;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ModulePathRegistryTest {

    @Test
    public void testParseAll() {
        String content;

        assertThat(ModulePathRegistry.parseAll(CustomName::new, ""))
                .isEmpty();

        content = "module hello {\n"
                + "    provides lib.HelloService with internal.lib.OldHelloService;\n"
                + "}\n";
        assertThat(ModulePathRegistry.parseAll(CustomName::new, content))
                .containsExactly(
                        newRef("lib.HelloService", "internal.lib.OldHelloService")
                );

        content = "module mymodule {\n"
                + "    provides lib.HelloService\n"
                + "            with internal.lib.NewHelloService;\n"
                + "}\n";
        assertThat(ModulePathRegistry.parseAll(CustomName::new, content))
                .containsExactly(
                        newRef("lib.HelloService", "internal.lib.NewHelloService")
                );

        content = "module mymodule {\n"
                + "    provides lib.HelloService with\n"
                + "            internal.lib.NewHelloService,\n"
                + "            internal.lib.OldHelloService, abc.xyz.Ab;\n"
                + "}\n";
        assertThat(ModulePathRegistry.parseAll(CustomName::new, content))
                .containsExactly(
                        newRef("lib.HelloService", "internal.lib.NewHelloService"),
                        newRef("lib.HelloService", "internal.lib.OldHelloService"),
                        newRef("lib.HelloService", "abc.xyz.Ab")
                );
    }
}
