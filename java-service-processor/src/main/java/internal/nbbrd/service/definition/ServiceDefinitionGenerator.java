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

import com.squareup.javapoet.*;
import internal.nbbrd.service.HasMethod;
import internal.nbbrd.service.Instantiator;
import internal.nbbrd.service.TypeNames;
import internal.nbbrd.service.Unreachable;
import nbbrd.service.Quantifier;

import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.squareup.javapoet.ClassName.OBJECT;
import static com.squareup.javapoet.TypeName.VOID;
import static internal.nbbrd.service.Blocks.*;
import static internal.nbbrd.service.TypeNames.*;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.*;

/**
 * @author Philippe Charles
 */
@lombok.Value
class ServiceDefinitionGenerator {

    private static final TypeVariableName BACKEND = TypeVariableName.get("BACKEND");

    public static List<ServiceDefinitionGenerator> allOf(
            List<LoadDefinition> definitions,
            Map<ClassName, List<LoadFilter>> filtersByService,
            Map<ClassName, List<LoadSorter>> sortersByService,
            Map<ClassName, List<LoadId>> idsByService) {
        return definitions
                .stream()
                .map(definition -> of(definition, filtersByService, sortersByService, idsByService))
                .collect(Collectors.toList());
    }

    public static ServiceDefinitionGenerator of(
            LoadDefinition definition,
            Map<ClassName, List<LoadFilter>> filtersByService,
            Map<ClassName, List<LoadSorter>> sortersByService,
            Map<ClassName, List<LoadId>> idsByService) {
        return new ServiceDefinitionGenerator(definition,
                filtersByService.getOrDefault(definition.getServiceType(), emptyList()),
                sortersByService.getOrDefault(definition.getServiceType(), emptyList()),
                idsByService.getOrDefault(definition.getServiceType(), emptyList())
        );
    }

    @lombok.NonNull
    LoadDefinition definition;

    @lombok.NonNull
    List<LoadFilter> filters;

    @lombok.NonNull
    List<LoadSorter> sorters;

    @lombok.NonNull
    List<LoadId> ids;

    public boolean hasCustomLoaderName() {
        return !definition.getLoaderName().isEmpty();
    }

    public TypeSpec generateLoader(boolean nested) {
        ClassName loaderName = ClassName.bestGuess(definition.resolveLoaderName().simpleName());
        ClassName builderName = ClassName.bestGuess("Builder");
        ClassName providerType = definition.getServiceType();
        ClassName batchTypeOrNull = definition.getBatchType().map(o -> ClassName.bestGuess(o.toString())).orElse(null);
        TypeName quantifierType = getQuantifierType();

        TypeSpec.Builder result = TypeSpec
                .classBuilder(loaderName)
                .addJavadoc(getMainJavadoc())
                .addModifiers(PUBLIC, FINAL);

        FieldSpec providerSource = FieldSpec
                .builder(iterableOf(WILDCARD), "providerSource", PRIVATE, FINAL)
                .build();

        FieldSpec providerReloader = FieldSpec
                .builder(Runnable.class, "providerReloader", PRIVATE, FINAL)
                .build();

        result.addField(providerSource);
        result.addField(providerReloader);

        MethodSpec reloadMethod;
        MethodSpec streamMethod;
        MethodSpec constructor;

        if (batchTypeOrNull != null) {
            FieldSpec batchSource = FieldSpec
                    .builder(iterableOf(WILDCARD), "batchSource", PRIVATE, FINAL)
                    .build();

            FieldSpec batchReloader = FieldSpec
                    .builder(Runnable.class, "batchReloader", PRIVATE, FINAL)
                    .build();

            reloadMethod = MethodSpec
                    .methodBuilder("reload")
                    .addJavadoc(CodeBlock
                            .builder()
                            .add("Reloads the content by clearing the cache and fetching available providers.\n")
                            .build())
                    .addModifiers(PUBLIC)
                    .returns(VOID)
                    .addStatement("$N.run()", providerReloader)
                    .addStatement("$N.run()", batchReloader)
                    .build();

            streamMethod = MethodSpec
                    .methodBuilder("stream")
                    .addModifiers(PRIVATE)
                    .returns(TypeNames.typeOf(Stream.class, providerType))
                    .addStatement(
                            CodeBlock
                                    .builder()
                                    .add("return ")
                                    .add(concatStreams(
                                            iterableToStream(providerSource, providerType),
                                            flatMapStream(iterableToStream(batchSource, batchTypeOrNull), CodeBlock.of("o -> o.getProviders()"))
                                    )).build())
                    .build();

            constructor = MethodSpec
                    .constructorBuilder()
                    .addModifiers(PRIVATE)
                    .addParameter(providerSource.type, providerSource.name)
                    .addParameter(providerReloader.type, providerReloader.name)
                    .addParameter(batchSource.type, batchSource.name)
                    .addParameter(batchReloader.type, batchReloader.name)
                    .addStatement("this.$N = $N", providerSource, providerSource)
                    .addStatement("this.$N = $N", providerReloader, providerReloader)
                    .addStatement("this.$N = $N", batchSource, batchSource)
                    .addStatement("this.$N = $N", batchReloader, batchReloader)
                    .build();

            result.addField(batchSource);
            result.addField(batchReloader);
        } else {
            reloadMethod = MethodSpec
                    .methodBuilder("reload")
                    .addJavadoc(CodeBlock
                            .builder()
                            .add("Reloads the content by clearing the cache and fetching available providers.\n")
                            .build())
                    .addModifiers(PUBLIC)
                    .returns(VOID)
                    .addStatement("$N.run()", providerReloader)
                    .build();

            streamMethod = MethodSpec
                    .methodBuilder("stream")
                    .addModifiers(PRIVATE)
                    .returns(TypeNames.typeOf(Stream.class, providerType))
                    .addStatement(
                            CodeBlock
                                    .builder()
                                    .add("return ")
                                    .add(iterableToStream(providerSource, providerType))
                                    .build())
                    .build();

            constructor = MethodSpec
                    .constructorBuilder()
                    .addModifiers(PRIVATE)
                    .addParameter(providerSource.type, providerSource.name)
                    .addParameter(providerReloader.type, providerReloader.name)
                    .addStatement("this.$N = $N", providerSource, providerSource)
                    .addStatement("this.$N = $N", providerReloader, providerReloader)
                    .build();
        }

        result.addMethod(reloadMethod);
        result.addMethod(streamMethod);
        result.addMethod(constructor);

        FieldSpec idPatternFieldOrNull = getIdPatternFieldOrNull();
        FieldSpec filterFieldOrNull = getFilterFieldOrNull(idPatternFieldOrNull);
        FieldSpec sorterFieldOrNull = getSorterFieldOrNull();

        if (idPatternFieldOrNull != null) result.addField(idPatternFieldOrNull);
        if (filterFieldOrNull != null) result.addField(filterFieldOrNull);
        if (sorterFieldOrNull != null) result.addField(sorterFieldOrNull);

        MethodSpec getMethod = MethodSpec
                .methodBuilder("get")
                .addJavadoc(getGetDescription())
                .addModifiers(PUBLIC)
                .returns(quantifierType)
                .addExceptions(getQuantifierException())
                .addStatement(CodeBlock
                        .builder()
                        .add("return ")
                        .add(getPreprocessingCode("stream", filterFieldOrNull, sorterFieldOrNull))
                        .add(getQuantifierCode())
                        .build())
                .build();

        result.addMethod(getMethod);

        MethodSpec builderMethod = MethodSpec
                .methodBuilder("builder")
                .addModifiers(PUBLIC, STATIC)
                .returns(builderName)
                .addStatement("return new $T()", builderName)
                .build();

        result.addMethod(newLoadMethod(quantifierType, getMethod));
        result.addMethod(builderMethod);
        result.addType(generateBuilder());

        return nested ? result.addModifiers(STATIC).build() : result.build();
    }

    public TypeSpec generateBuilder() {
        ClassName loaderName = ClassName.bestGuess(definition.resolveLoaderName().simpleName());
        ClassName builderName = ClassName.bestGuess("Builder");
        ClassName batchTypeOrNull = definition.getBatchType().map(o -> ClassName.bestGuess(o.toString())).orElse(null);

        FieldSpec factoryField = FieldSpec
                .builder(functionOf(WILDCARD_CLASS, OBJECT), "factory", PRIVATE)
                .initializer("$T::load", ServiceLoader.class)
                .build();

        FieldSpec streamerField = FieldSpec
                .builder(functionOf(OBJECT, iterableOf(WILDCARD)), "streamer", PRIVATE)
                .initializer("backend -> (($T) backend)", ServiceLoader.class)
                .build();

        FieldSpec reloaderField = FieldSpec
                .builder(consumerOf(OBJECT), "reloader", PRIVATE)
                .initializer("backend -> (($T) backend).reload()", ServiceLoader.class)
                .build();

        MethodSpec backendMethod = MethodSpec
                .methodBuilder("backend")
                .addModifiers(PUBLIC)
                .addTypeVariable(BACKEND)
                .returns(builderName)
                .addParameter(functionOf(WILDCARD_CLASS, BACKEND), "factory")
                .addParameter(functionOf(BACKEND, iterableOf(WILDCARD)), "streamer")
                .addParameter(consumerOf(BACKEND), "reloader")
                .addStatement("this.$N = ($T) factory", factoryField, functionOf(WILDCARD_CLASS, OBJECT))
                .addStatement("this.$N = ($T) streamer", streamerField, functionOf(OBJECT, iterableOf(WILDCARD)))
                .addStatement("this.$N = ($T) reloader", reloaderField, consumerOf(OBJECT))
                .addStatement("return this")
                .build();

        MethodSpec.Builder buildMethod = MethodSpec
                .methodBuilder("build")
                .addModifiers(PUBLIC)
                .returns(loaderName);

        if (batchTypeOrNull != null) {
            buildMethod.addStatement("$T providerBackend = factory.apply($T.class)", OBJECT, definition.getServiceType());
            buildMethod.addStatement("$T batchBackend = factory.apply($T.class)", OBJECT, batchTypeOrNull);
            buildMethod.addStatement(
                    CodeBlock
                            .builder()
                            .add("return new $T(", loaderName).add(NEW_LINE)
                            .add("$N.apply(providerBackend), () -> $N.accept(providerBackend),", streamerField, reloaderField).add(NEW_LINE)
                            .add("$N.apply(batchBackend), () -> $N.accept(batchBackend)", streamerField, reloaderField).add(NEW_LINE)
                            .add(")")
                            .build()
            );
        } else {
            buildMethod.addStatement("$T providerBackend = factory.apply($T.class)", OBJECT, definition.getServiceType());
            buildMethod.addStatement(
                    CodeBlock
                            .builder()
                            .add("return new $T(", loaderName).add(NEW_LINE)
                            .add("$N.apply(providerBackend), () -> $N.accept(providerBackend)", streamerField, reloaderField).add(NEW_LINE)
                            .add(")")
                            .build()
            );
        }

        return TypeSpec
                .classBuilder(builderName)
                .addModifiers(PUBLIC, STATIC, FINAL)
                .addField(factoryField)
                .addField(streamerField)
                .addField(reloaderField)
                .addMethod(backendMethod)
                .addMethod(buildMethod.build())
                .build();
    }

    private CodeBlock getMainJavadoc() {
        return CodeBlock
                .builder()
                .add("Custom service loader for $L.\n", toJavadocLink(definition.getServiceType()))
                .add("<p>Properties:\n")
                .add("<ul>\n")
                .add("<li>Quantifier: $L</li>\n", definition.getQuantifier())
                .add("<li>Fallback: $L</li>\n", toJavadocLink(definition.getFallback().orElse(null)))
                .add("<li>Preprocessing: $L</li>\n", getPreprocessingJavadoc())
                .add("<li>Name: $L</li>\n", definition.getLoaderName().isEmpty() ? "null" : definition.getLoaderName())
                .add("<li>Batch type: $L</li>\n", definition.getBatchType().map(TypeMirror::toString).orElse("null"))
                .add("</ul>\n")
                .build();
    }

    private String getPreprocessingJavadoc() {
        if (!filters.isEmpty() || !sorters.isEmpty()) {
            return "filters:" + filters.stream().collect(toMethodNames())
                    + " sorters:" + sorters.stream().collect(toMethodNames());
        }
        return "null";
    }

    private CodeBlock getPreprocessingCode(
            String gatherMethod,
            FieldSpec filterFieldOrNull,
            FieldSpec sorterFieldOrNull
    ) {
        CodeBlock.Builder result = CodeBlock.builder();
        result.add("$L()", gatherMethod);
        if (filterFieldOrNull != null) result.add(NEW_LINE).add(".filter($L)", filterFieldOrNull.name);
        if (sorterFieldOrNull != null) result.add(NEW_LINE).add(".sorted($L)", sorterFieldOrNull.name);
        return result.build();
    }

    private CodeBlock getIdPredicateCode(FieldSpec field) {
        return CodeBlock.of("o -> $N.matcher(o.$L()).matches()", field, ids.get(0).getMethod().getSimpleName());
    }

    private CodeBlock getFiltersCode(FieldSpec idPatternFieldOrNull) {
        List<CodeBlock> blocks = new ArrayList<>();
        if (idPatternFieldOrNull != null) blocks.add(getIdPredicateCode(idPatternFieldOrNull));
        filters.stream()
                .sorted(Comparator.comparingInt(LoadFilter::getPosition))
                .map(this::getFilterCode)
                .forEach(blocks::add);

        Iterator<CodeBlock> iterator = blocks.iterator();

        CodeBlock first = iterator.next();

        if (!iterator.hasNext()) {
            return first;
        }

        CodeBlock.Builder result = CodeBlock.builder();
        result.add(casting(TypeNames.typeOf(Predicate.class, definition.getServiceType()), first));
        while (iterator.hasNext()) {
            result.add(".and($L)", iterator.next());
        }
        return result.build();
    }

    private CodeBlock getFilterCode(LoadFilter filter) {
        CodeBlock result = CodeBlock.of("$T::$L", filter.getServiceType().orElseThrow(Unreachable::new), filter.getMethodName());
        return filter.isNegate()
                ? CodeBlock.of("$L.negate()", casting(TypeNames.typeOf(Predicate.class, definition.getServiceType()), result))
                : result;
    }

    private CodeBlock getSortersCode() {
        Iterator<CodeBlock> iter = sorters.stream()
                .sorted(Comparator.comparingInt(LoadSorter::getPosition))
                .map(this::getSorterCode)
                .iterator();

        CodeBlock first = iter.next();

        if (!iter.hasNext()) {
            return first;
        }

        CodeBlock.Builder result = CodeBlock.builder();
        result.add(casting(TypeNames.typeOf(Comparator.class, definition.getServiceType()), first));
        while (iter.hasNext()) {
            result.add(".thenComparing($L)", iter.next());
        }
        return result.build();
    }

    private CodeBlock getSorterCode(LoadSorter sorter) {
        CodeBlock result = CodeBlock.of("$T.$L($T::$L)", Comparator.class, getComparatorMethod(sorter), sorter.getServiceType().orElseThrow(Unreachable::new), sorter.getMethodName());
        return sorter.isReverse()
                ? CodeBlock.of("$T.reverseOrder($L)", Collections.class, result)
                : result;
    }

    private String getComparatorMethod(LoadSorter sorter) {
        switch (sorter.getKeyType().orElseThrow(Unreachable::new)) {
            case COMPARABLE:
                return "comparing";
            case DOUBLE:
                return "comparingDouble";
            case INT:
                return "comparingInt";
            case LONG:
                return "comparingLong";
            default:
                throw new Unreachable();
        }
    }

    private CodeBlock getQuantifierCode() {
        switch (definition.getQuantifier()) {
            case OPTIONAL:
                return CodeBlock.of("\n.findFirst()");
            case SINGLE:
                return definition.getFallback().isPresent()
                        ? CodeBlock.of("\n.findFirst()\n.orElseGet(() -> $L)", getInstantiatorCode(definition.getFallback().get()))
                        : CodeBlock.of("\n.findFirst()\n.orElseThrow(() -> new $T(\"Missing mandatory provider of $T\"))", IllegalStateException.class, definition.getServiceType());
            case MULTIPLE:
                return CodeBlock.of("\n.collect($T.collectingAndThen($T.toList(), $T::unmodifiableList))", Collectors.class, Collectors.class, Collections.class);
            default:
                throw new Unreachable();
        }
    }

    private CodeBlock getInstantiatorCode(TypeInstantiator instance) {
        Instantiator instantiator = instance.select().orElseThrow(RuntimeException::new);
        switch (instantiator.getKind()) {
            case CONSTRUCTOR:
                return CodeBlock.of("new $T()", instance.getType());
            case STATIC_METHOD:
                return CodeBlock.of("$T.$L()", instance.getType(), instantiator.getElement().getSimpleName());
            case ENUM_FIELD:
            case STATIC_FIELD:
                return CodeBlock.of("$T.$L", instance.getType(), instantiator.getElement().getSimpleName());
            default:
                throw new Unreachable();
        }
    }

    private TypeName getQuantifierType() {
        switch (definition.getQuantifier()) {
            case OPTIONAL:
                return TypeNames.typeOf(Optional.class, definition.getServiceType());
            case SINGLE:
                return definition.getServiceType();
            case MULTIPLE:
                return TypeNames.typeOf(List.class, definition.getServiceType());
            default:
                throw new Unreachable();
        }
    }

    private List<TypeName> getQuantifierException() {
        return definition.getQuantifier() == Quantifier.SINGLE && !definition.getFallback().isPresent()
                ? Collections.singletonList(ClassName.get(IllegalStateException.class))
                : emptyList();
    }

    private FieldSpec getSourceField() {
        return FieldSpec
                .builder(TypeNames.typeOf(Iterable.class, definition.getServiceType()), "source")
                .addModifiers(PRIVATE, FINAL)
                .initializer("$L", getBackendInitCode(definition.getServiceType()))
                .build();
    }

    private FieldSpec getBatchFieldOrNull() {
        if (definition.getBatchType().isPresent()) {
            ClassName batchTypeName = ClassName.bestGuess(definition.getBatchType().get().toString());
            return FieldSpec
                    .builder(TypeNames.typeOf(Iterable.class, batchTypeName), "batch")
                    .addModifiers(PRIVATE, FINAL)
                    .initializer("$L", getBackendInitCode(batchTypeName))
                    .build();
        }
        return null;
    }

    private FieldSpec getFilterFieldOrNull(FieldSpec idPatternFieldOrNull) {
        return !filters.isEmpty() || idPatternFieldOrNull != null
                ? FieldSpec
                  .builder(TypeNames.typeOf(Predicate.class, definition.getServiceType()), "filter")
                  .addModifiers(PRIVATE, FINAL)
                  .initializer("$L", getFiltersCode(idPatternFieldOrNull))
                  .build()
                : null;
    }

    private FieldSpec getSorterFieldOrNull() {
        return !sorters.isEmpty()
                ? FieldSpec
                  .builder(TypeNames.typeOf(Comparator.class, definition.getServiceType()), "sorter")
                  .addModifiers(PRIVATE, FINAL)
                  .initializer("$L", getSortersCode())
                  .build()
                : null;
    }

    private CodeBlock getBackendInitCode(ClassName serviceType) {
        return CodeBlock.of("$T.load($T.class)", ServiceLoader.class, serviceType);
    }

    private CodeBlock getGetDescription() {
        switch (definition.getQuantifier()) {
            case OPTIONAL:
                return CodeBlock.of("Gets an optional $L instance.\n", toJavadocLink(definition.getServiceType()));
            case SINGLE:
                return CodeBlock.of("Gets a $L instance.\n", toJavadocLink(definition.getServiceType()));
            case MULTIPLE:
                return CodeBlock.of("Gets a list of $L instances.\n", toJavadocLink(definition.getServiceType()));
            default:
                throw new Unreachable();
        }
    }

    private MethodSpec newLoadMethod(TypeName quantifierType, MethodSpec getter) {
        CodeBlock mainStatement = CodeBlock.of("builder().build().$N()", getter);

        MethodSpec.Builder result = MethodSpec
                .methodBuilder("load")
                .addJavadoc(CodeBlock
                        .builder()
                        .add(getGetDescription())
                        .add("<br>This is equivalent to the following code: <code>$L</code>\n", mainStatement)
                        .add("<br>Therefore, the returned value might be different at each call.\n")
                        .add("@return a non-null value\n")
                        .build())
                .addModifiers(PUBLIC, STATIC)
                .returns(quantifierType)
                .addExceptions(getQuantifierException());

        result.addStatement("return $L", mainStatement);

        return result.build();
    }

    private FieldSpec getIdPatternFieldOrNull() {
        return ids.size() == 1 && !ids.get(0).getPattern().isEmpty()
                ? FieldSpec
                  .builder(Pattern.class, "ID_PATTERN")
                  .addModifiers(PUBLIC, STATIC, FINAL)
                  .initializer("$T.compile(\"$N\")", Pattern.class, ids.get(0).getPattern())
                  .build()
                : null;
    }

    private static String toJavadocLink(ClassName type) {
        return "{@link " + type + "}";
    }

    private static String toJavadocLink(TypeInstantiator typeOrNull) {
        return typeOrNull != null ? ("{@link " + typeOrNull.getType() + "}") : "null";
    }

    private static Collector<HasMethod, ?, String> toMethodNames() {
        return Collectors.mapping(HasMethod::getMethodName, Collectors.joining("+", "[", "]"));
    }

    private static final CodeBlock NEW_LINE = CodeBlock.of("\n");
}
