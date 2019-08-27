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

import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.CharStreams;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ModuleInfoEntriesTest {

    @Test
    public void testParseAll() throws IOException {
        String content;

        assertThat(ModuleInfoEntries.parse(""))
                .isEqualTo(ModuleInfoEntries.builder().build());

        content = "module hello {\n"
                + "    provides lib.HelloService with internal.lib.OldHelloService;\n"
                + "    uses lib.HelloService;\n"
                + "}\n";
        assertThat(ModuleInfoEntries.parse(content))
                .isEqualTo(ModuleInfoEntries
                        .builder()
                        .provision("lib.HelloService", "internal.lib.OldHelloService")
                        .usage("lib.HelloService")
                        .build()
                );

        content = "module mymodule {\n"
                + "    provides lib.HelloService\n"
                + "            with internal.lib.NewHelloService;\n"
                + "}\n";
        assertThat(ModuleInfoEntries.parse(content))
                .isEqualTo(ModuleInfoEntries
                        .builder()
                        .provision("lib.HelloService", "internal.lib.NewHelloService")
                        .build()
                );

        content = "module mymodule {\n"
                + "    provides lib.HelloService with\n"
                + "            internal.lib.NewHelloService,\n"
                + "            internal.lib.OldHelloService, abc.xyz.Ab;\n"
                + "}\n";
        assertThat(ModuleInfoEntries.parse(content))
                .isEqualTo(ModuleInfoEntries
                        .builder()
                        .provision("lib.HelloService", "internal.lib.NewHelloService")
                        .provision("lib.HelloService", "internal.lib.OldHelloService")
                        .provision("lib.HelloService", "abc.xyz.Ab")
                        .build()
                );

        content = "import lib.HelloService;\n"
                + "import internal.lib.NewHelloService;\n"
                + "import sandbox.samples.Logging;\n"
                + "module mymodule {\n"
                + "    provides lib.HelloService with NewHelloService;\n"
                + "    uses HelloService;\n"
                + "    uses Logging.LoggerSpi;\n"
                + "}\n";
        assertThat(ModuleInfoEntries.parse(content))
                .isEqualTo(ModuleInfoEntries
                        .builder()
                        .provision("lib.HelloService", "internal.lib.NewHelloService")
                        .usage("lib.HelloService")
                        .usage("sandbox.samples.Logging.LoggerSpi")
                        .build()
                );

        try (InputStream stream = ModuleInfoEntriesTest.class.getResourceAsStream("/provider/somemodule-info.java")) {
            assertThat(ModuleInfoEntries.parse(CharStreams.fromStream(stream)))
                    .isEqualTo(ModuleInfoEntries
                            .builder()
                            .provision("java.util.spi.LocaleServiceProvider", "internal.pac.modern.lib.NewModernService")
                            .provision("java.util.spi.LocaleServiceProvider", "internal.pac.modern.lib.OldModernService")
                            .build()
                    );
        }
    }
}
