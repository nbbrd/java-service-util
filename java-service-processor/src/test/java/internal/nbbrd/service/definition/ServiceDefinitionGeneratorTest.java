package internal.nbbrd.service.definition;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import nbbrd.service.Quantifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ServiceDefinitionGeneratorTest {

    private static final ClassName SERVICE_TYPE = ClassName.get("com.example", "MyService");

    private static LoadDefinition baseDefinition(ClassName serviceType, Quantifier quantifier) {
        return LoadDefinition.builder()
                .quantifier(quantifier)
                .serviceType(serviceType)
                .fallback(Optional.empty())
                .loaderName("")
                .batch(Optional.empty())
                .build();
    }

    private static ServiceDefinitionGenerator generatorOf(LoadDefinition definition) {
        return new ServiceDefinitionGenerator(definition, emptyList(), emptyList(), emptyList());
    }

    @Nested
    class HasCustomLoaderNameTest {

        @Test
        public void returnsFalseWhenLoaderNameIsEmpty() {
            ServiceDefinitionGenerator gen = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL));
            assertThat(gen.hasCustomLoaderName()).isFalse();
        }

        @Test
        public void returnsTrueWhenLoaderNameIsNonEmpty() {
            LoadDefinition definition = LoadDefinition.builder()
                    .quantifier(Quantifier.OPTIONAL)
                    .serviceType(SERVICE_TYPE)
                    .fallback(Optional.empty())
                    .loaderName("internal.CustomLoader")
                    .batch(Optional.empty())
                    .build();
            assertThat(generatorOf(definition).hasCustomLoaderName()).isTrue();
        }
    }

    @Nested
    class GenerateLoaderTest {

        @Test
        public void generatesPublicFinalNonStaticClassForTopLevel() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
            assertThat(typeSpec.modifiers).contains(PUBLIC, FINAL);
            assertThat(typeSpec.modifiers).doesNotContain(STATIC);
        }

        @Test
        public void generatesPublicFinalStaticClassWhenNested() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(true);
            assertThat(typeSpec.modifiers).contains(PUBLIC, FINAL, STATIC);
        }

        @Test
        public void generatesClassNameByAppendingLoaderSuffixToServiceType() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
            assertThat(typeSpec.name).isEqualTo("MyServiceLoader");
        }

        @Test
        public void usesSimpleNameOfCustomLoaderName() {
            LoadDefinition definition = LoadDefinition.builder()
                    .quantifier(Quantifier.OPTIONAL)
                    .serviceType(SERVICE_TYPE)
                    .fallback(Optional.empty())
                    .loaderName("internal.FooLoader")
                    .batch(Optional.empty())
                    .build();
            TypeSpec typeSpec = generatorOf(definition).generateLoader(false);
            assertThat(typeSpec.name).isEqualTo("FooLoader");
        }

        @Test
        public void alwaysIncludesProviderSourceField() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
            assertThat(typeSpec.fieldSpecs)
                    .extracting(f -> f.name)
                    .contains("providerSource");
        }

        @Test
        public void doesNotIncludeBatchFieldWhenNoBatchTypeDefined() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
            assertThat(typeSpec.fieldSpecs)
                    .extracting(f -> f.name)
                    .doesNotContain("batch");
        }

        @Test
        public void doesNotIncludeFilterFieldWhenNoFiltersOrIds() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.MULTIPLE)).generateLoader(false);
            assertThat(typeSpec.fieldSpecs)
                    .extracting(f -> f.name)
                    .doesNotContain("filter");
        }

        @Test
        public void doesNotIncludeSorterFieldWhenNoSorters() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.MULTIPLE)).generateLoader(false);
            assertThat(typeSpec.fieldSpecs)
                    .extracting(f -> f.name)
                    .doesNotContain("sorter");
        }

        @Test
        public void alwaysIncludesGetMethod() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
            assertThat(typeSpec.methodSpecs)
                    .extracting(m -> m.name)
                    .contains("get");
        }

        @Test
        public void alwaysIncludesPublicReloadMethod() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
            assertThat(typeSpec.methodSpecs)
                    .filteredOn(m -> m.name.equals("reload"))
                    .singleElement()
                    .extracting(m -> m.modifiers)
                    .satisfies(modifiers -> assertThat(modifiers).contains(PUBLIC).doesNotContain(STATIC));
        }

        @Test
        public void doesNotIncludeGetByIdMethodWhenNoIds() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.MULTIPLE)).generateLoader(false);
            assertThat(typeSpec.methodSpecs)
                    .extracting(m -> m.name)
                    .doesNotContain("getById", "loadById");
        }

        @Test
        public void doesNotIncludeGetByIdMethodWhenQuantifierIsNotMultiple() {
            for (Quantifier quantifier : new Quantifier[]{Quantifier.OPTIONAL, Quantifier.SINGLE}) {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, quantifier)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .extracting(m -> m.name)
                        .doesNotContain("getById", "loadById");
            }
        }

        @Test
        public void alwaysIncludesPublicStaticLoadMethod() {
            TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
            assertThat(typeSpec.methodSpecs)
                    .filteredOn(m -> m.name.equals("load"))
                    .singleElement()
                    .extracting(m -> m.modifiers)
                    .satisfies(modifiers -> assertThat(modifiers).contains(PUBLIC, STATIC));
        }

        @Nested
        class QuantifierReturnTypeTest {

            @Test
            public void optionalQuantifierProducesOptionalReturnType() {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .filteredOn(m -> m.name.equals("get"))
                        .singleElement()
                        .extracting(m -> m.returnType.toString())
                        .isEqualTo("java.util.Optional<com.example.MyService>");
            }

            @Test
            public void singleQuantifierProducesDirectServiceTypeAsReturnType() {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.SINGLE)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .filteredOn(m -> m.name.equals("get"))
                        .singleElement()
                        .extracting(m -> m.returnType.toString())
                        .isEqualTo("com.example.MyService");
            }

            @Test
            public void multipleQuantifierProducesListReturnType() {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.MULTIPLE)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .filteredOn(m -> m.name.equals("get"))
                        .singleElement()
                        .extracting(m -> m.returnType.toString())
                        .isEqualTo("java.util.List<com.example.MyService>");
            }

            @Test
            public void loadMethodReturnTypeMatchesGetMethodReturnType() {
                for (Quantifier quantifier : Quantifier.values()) {
                    TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, quantifier)).generateLoader(false);
                    String getReturn = typeSpec.methodSpecs.stream()
                            .filter(m -> m.name.equals("get"))
                            .findFirst().orElseThrow(AssertionError::new)
                            .returnType.toString();
                    assertThat(typeSpec.methodSpecs)
                            .filteredOn(m -> m.name.equals("load"))
                            .singleElement()
                            .extracting(m -> m.returnType.toString())
                            .isEqualTo(getReturn);
                }
            }
        }

        @Nested
        class ExceptionDeclarationTest {

            @Test
            public void singleQuantifierWithoutFallbackDeclaresThrownIllegalStateException() {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.SINGLE)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .filteredOn(m -> m.name.equals("get"))
                        .singleElement()
                        .satisfies(m -> assertThat(m.exceptions).contains(ClassName.get(IllegalStateException.class)));
            }

            @Test
            public void singleQuantifierWithoutFallbackDeclaresExceptionOnLoadMethod() {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.SINGLE)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .filteredOn(m -> m.name.equals("load"))
                        .singleElement()
                        .satisfies(m -> assertThat(m.exceptions).contains(ClassName.get(IllegalStateException.class)));
            }

            @Test
            public void optionalQuantifierDoesNotDeclareAnyException() {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .filteredOn(m -> m.name.equals("get"))
                        .singleElement()
                        .satisfies(m -> assertThat(m.exceptions).isEmpty());
            }

            @Test
            public void multipleQuantifierDoesNotDeclareAnyException() {
                TypeSpec typeSpec = generatorOf(baseDefinition(SERVICE_TYPE, Quantifier.MULTIPLE)).generateLoader(false);
                assertThat(typeSpec.methodSpecs)
                        .filteredOn(m -> m.name.equals("get"))
                        .singleElement()
                        .satisfies(m -> assertThat(m.exceptions).isEmpty());
            }
        }
    }

    @Nested
    class AllOfTest {

        @Test
        public void returnsEmptyListForEmptyDefinitions() {
            assertThat(ServiceDefinitionGenerator.allOf(
                    emptyList(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap()))
                    .isEmpty();
        }

        @Test
        public void returnsOneGeneratorPerDefinition() {
            ClassName type1 = ClassName.get("com.example", "ServiceA");
            ClassName type2 = ClassName.get("com.example", "ServiceB");

            assertThat(ServiceDefinitionGenerator.allOf(
                    Arrays.asList(
                            baseDefinition(type1, Quantifier.OPTIONAL),
                            baseDefinition(type2, Quantifier.MULTIPLE)
                    ),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap()))
                    .hasSize(2)
                    .extracting(g -> g.getDefinition().getServiceType())
                    .containsExactly(type1, type2);
        }

        @Test
        public void preservesDefinitionOrderInResult() {
            ClassName type1 = ClassName.get("com.example", "First");
            ClassName type2 = ClassName.get("com.example", "Second");
            ClassName type3 = ClassName.get("com.example", "Third");

            assertThat(ServiceDefinitionGenerator.allOf(
                    Arrays.asList(
                            baseDefinition(type1, Quantifier.OPTIONAL),
                            baseDefinition(type2, Quantifier.SINGLE),
                            baseDefinition(type3, Quantifier.MULTIPLE)
                    ),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap()))
                    .extracting(g -> g.getDefinition().getServiceType())
                    .containsExactly(type1, type2, type3);
        }
    }

    @Nested
    class OfTest {

        @Test
        public void usesEmptyListsWhenNoMappingsExistForService() {
            LoadDefinition definition = baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL);
            ServiceDefinitionGenerator gen = ServiceDefinitionGenerator.of(
                    definition,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap());
            assertThat(gen.getFilters()).isEmpty();
            assertThat(gen.getSorters()).isEmpty();
            assertThat(gen.getIds()).isEmpty();
        }

        @Test
        public void carriesTheOriginalDefinition() {
            LoadDefinition definition = baseDefinition(SERVICE_TYPE, Quantifier.OPTIONAL);
            ServiceDefinitionGenerator gen = ServiceDefinitionGenerator.of(
                    definition,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap());
            assertThat(gen.getDefinition()).isSameAs(definition);
        }
    }
}


