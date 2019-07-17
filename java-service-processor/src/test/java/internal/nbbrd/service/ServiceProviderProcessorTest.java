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

import internal.nbbrd.service.ServiceProviderProcessor.ClassPathRegistry;
import internal.nbbrd.service.ServiceProviderProcessor.ModulePathRegistry;
import javax.lang.model.element.Name;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ServiceProviderProcessorTest {

    @Test
    public void testClassPathRegistryParse() {
        String content;

        content = "internal.pac.legacy.lib.OldHelloService";
        assertThat(ClassPathRegistry.parse(CustomName::new, new CustomName("pac.legacy.lib.HelloService"), content))
                .isEqualTo(newRef("pac.legacy.lib.HelloService", "internal.pac.legacy.lib.OldHelloService"));

        content = "internal.pac.legacy.lib.OldHelloService # comment";
        assertThat(ClassPathRegistry.parse(CustomName::new, new CustomName("pac.legacy.lib.HelloService"), content))
                .isEqualTo(newRef("pac.legacy.lib.HelloService", "internal.pac.legacy.lib.OldHelloService"));
    }

    @Test
    public void testModulePathRegistryParseAll() {
        String content;

        content = "module mod.legacy.lib {\n"
                + "    provides pac.legacy.lib.HelloService with internal.pac.legacy.lib.OldHelloService;\n"
                + "}\n";
        assertThat(ModulePathRegistry.parseAll(CustomName::new, content))
                .containsExactly(
                        newRef("pac.legacy.lib.HelloService", "internal.pac.legacy.lib.OldHelloService")
                );

        content = "module mod.legacy.lib {\n"
                + "    provides pac.legacy.lib.HelloService\n"
                + "            with internal.pac.legacy.lib.NewHelloService;\n"
                + "}\n";
        assertThat(ModulePathRegistry.parseAll(CustomName::new, content))
                .containsExactly(
                        newRef("pac.legacy.lib.HelloService", "internal.pac.legacy.lib.NewHelloService")
                );

        content = "module mod.legacy.lib {\n"
                + "    provides pac.legacy.lib.HelloService with\n"
                + "            internal.pac.legacy.lib.NewHelloService,\n"
                + "            internal.pac.legacy.lib.OldHelloService, abc.xyz.Ab;\n"
                + "}\n";
        assertThat(ModulePathRegistry.parseAll(CustomName::new, content))
                .containsExactly(
                        newRef("pac.legacy.lib.HelloService", "internal.pac.legacy.lib.NewHelloService"),
                        newRef("pac.legacy.lib.HelloService", "internal.pac.legacy.lib.OldHelloService"),
                        newRef("pac.legacy.lib.HelloService", "abc.xyz.Ab")
                );
    }

    private static ServiceProviderProcessor.ProviderRef newRef(String service, String provider) {
        return new ServiceProviderProcessor.ProviderRef(new CustomName(service), new CustomName(provider));
    }

    @lombok.Value
    private static class CustomName implements Name {

        @lombok.experimental.Delegate
        private final String content;

        @Override
        public String toString() {
            return content;
        }
    }
}
