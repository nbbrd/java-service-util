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
package internal.nbbrd.service.definition;

import com.squareup.javapoet.ClassName;
import org.junit.jupiter.api.Test;

import static internal.nbbrd.service.definition.LoadDefinition.resolveName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class LoadDefinitionTest {

    @Test
    public void testResolveNameForNonNestedService() {
        ClassName nonNestedService = ClassName.get("nbbrd.charts", "ColorScheme");

        assertThat(resolveName("", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("nbbrd.charts", "ColorSchemeStuff"));

        assertThat(resolveName("internal.charts.ColorHandler", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal.charts", "ColorHandler"));
    }

    @Test
    public void testResolveNameForNestedService() {
        ClassName nestedService = ClassName.get("nbbrd.charts", "Utils", "ColorScheme");

        assertThat(resolveName("", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("nbbrd.charts", "UtilsStuff", "ColorScheme"));

        assertThat(resolveName("internal.charts.ColorHandler", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal.charts", "ColorHandler"));
    }
}
