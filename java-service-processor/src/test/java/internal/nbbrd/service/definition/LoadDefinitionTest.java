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

import static internal.nbbrd.service.definition.LoadDefinition.NO_NAME;
import static internal.nbbrd.service.definition.LoadDefinition.resolveName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Philippe Charles
 */
public class LoadDefinitionTest {

    @Test
    public void testResolveNameForNonNestedService() {
        ClassName nonNestedService = ClassName.get("node1.node2", "Leaf");

        assertThat(resolveName(NO_NAME, nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("node1.node2", "LeafStuff"));

        assertThat(resolveName("internal.CustomLeaf", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal", "CustomLeaf"));

        assertThat(resolveName("{{packageName}}.CustomLeaf", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("node1.node2", "CustomLeaf"));

        assertThat(resolveName("internal.{{packageName}}.{{simpleName}}Loader", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal.node1.node2", "LeafLoader"));

        assertThat(resolveName("internal.{{canonicalName}}Loader", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal.node1.node2", "LeafLoader"));
    }

    @Test
    public void testResolveNameForNonNestedServiceNoPackage() {
        ClassName nonNestedService = ClassName.get("", "Leaf");

        assertThat(resolveName(NO_NAME, nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("", "LeafStuff"));

        assertThat(resolveName("internal.CustomLeaf", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal", "CustomLeaf"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolveName("{{packageName}}.CustomLeaf", nonNestedService, "Stuff"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolveName("internal.{{packageName}}.{{simpleName}}Loader", nonNestedService, "Stuff"));

        assertThat(resolveName("internal.{{canonicalName}}Loader", nonNestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal", "LeafLoader"));
    }

    @Test
    public void testResolveNameForNestedService() {
        ClassName nestedService = ClassName.get("node1.node2", "Leaf", "NestedLeaf");

        assertThat(resolveName(NO_NAME, nestedService, "Stuff"))
                .isEqualTo(ClassName.get("node1.node2", "LeafStuff", "NestedLeaf"));

        assertThat(resolveName("internal.CustomLeaf", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal", "CustomLeaf"));

        assertThat(resolveName("{{packageName}}.CustomLeaf", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("node1.node2", "CustomLeaf"));

        assertThat(resolveName("internal.{{packageName}}.{{simpleName}}Loader", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal.node1.node2", "NestedLeafLoader"));

        assertThat(resolveName("internal.{{canonicalName}}Loader", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal.node1.node2.Leaf", "NestedLeafLoader"));
    }

    @Test
    public void testResolveNameForNestedServiceNoPackage() {
        ClassName nestedService = ClassName.get("", "Leaf", "NestedLeaf");

        assertThat(resolveName(NO_NAME, nestedService, "Stuff"))
                .isEqualTo(ClassName.get("", "LeafStuff", "NestedLeaf"));

        assertThat(resolveName("internal.CustomLeaf", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal", "CustomLeaf"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolveName("{{packageName}}.CustomLeaf", nestedService, "Stuff"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolveName("internal.{{packageName}}.{{simpleName}}Loader", nestedService, "Stuff"));

        assertThat(resolveName("internal.{{canonicalName}}Loader", nestedService, "Stuff"))
                .isEqualTo(ClassName.get("internal.Leaf", "NestedLeafLoader"));
    }
}
