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

import internal.nbbrd.service.ModuleInfoEntries;
import static internal.nbbrd.service.provider.ModulePathRegistry.parseAll;
import java.io.IOException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ModulePathRegistryTest {

    @Test
    public void testParseAll() throws IOException {
        ModuleInfoEntries content;

        content = ModuleInfoEntries.builder().build();
        assertThat(parseAll(content))
                .isEmpty();

        content = ModuleInfoEntries
                .builder()
                .provision("lib.HelloService", "internal.lib.OldHelloService")
                .build();
        assertThat(parseAll(content))
                .containsExactly(
                        new ProviderEntry("lib.HelloService", "internal.lib.OldHelloService")
                );

        content = ModuleInfoEntries
                .builder()
                .provision("lib.HelloService", "internal.lib.NewHelloService")
                .build();
        assertThat(parseAll(content))
                .containsExactly(
                        new ProviderEntry("lib.HelloService", "internal.lib.NewHelloService")
                );

        content = ModuleInfoEntries
                .builder()
                .provision("lib.HelloService", "internal.lib.NewHelloService")
                .provision("lib.HelloService", "internal.lib.OldHelloService")
                .provision("lib.HelloService", "abc.xyz.Ab")
                .build();
        assertThat(parseAll(content))
                .containsExactly(
                        new ProviderEntry("lib.HelloService", "internal.lib.NewHelloService"),
                        new ProviderEntry("lib.HelloService", "internal.lib.OldHelloService"),
                        new ProviderEntry("lib.HelloService", "abc.xyz.Ab")
                );
    }
}
