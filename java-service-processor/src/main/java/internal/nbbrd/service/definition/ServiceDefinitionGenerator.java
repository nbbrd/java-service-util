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

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        ClassName batchTypeOrNull = definition.getBatch().map(o -> ClassName.bestGuess(o.getType().toString())).orElse(null);
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
            BatchDefinition batchDefinition = definition.getBatch().orElseThrow(Unreachable::new);

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
                            .add("<p>This method reloads both individual providers and batch providers.\n")
                            .add("It should be called when the set of available providers may have changed.\n")
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
                                            flatMapStream(iterableToStream(batchSource, batchTypeOrNull), getBatchMapper(batchDefinition))
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
                            .add("<p>It should be called when the set of available providers may have changed.\n")
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
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Creates a new builder to configure and construct a loader instance.\n")
                        .add("<p>Use this method to customize the backend (e.g. NetBeans Lookup) instead of the default $T.\n", ServiceLoader.class)
                        .add("@return a non-null new $T instance\n", ClassName.bestGuess("Builder"))
                        .build())
                .addModifiers(PUBLIC, STATIC)
                .returns(builderName)
                .addStatement("return new $T()", builderName)
                .build();

        result.addMethod(newLoadMethod(quantifierType, getMethod));
        if (!ids.isEmpty() && definition.getQuantifier() == Quantifier.MULTIPLE) {
            result.addMethod(newGetByIdMethod(filterFieldOrNull));
            result.addMethod(newLoadByIdMethod());
        }
        result.addMethod(builderMethod);
        result.addType(generateBuilder());

        return nested ? result.addModifiers(STATIC).build() : result.build();
    }

    public TypeSpec generateBuilder() {
        ClassName loaderName = ClassName.bestGuess(definition.resolveLoaderName().simpleName());
        ClassName builderName = ClassName.bestGuess("Builder");
        ClassName batchTypeOrNull = definition.getBatch().map(o -> ClassName.bestGuess(o.getType().toString())).orElse(null);

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

        MethodSpec backendMethod1 = MethodSpec
                .methodBuilder("backend")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Configures a custom backend for loading and reloading providers.\n")
                        .add("@param factory a function that creates a backend instance from a service class, not null\n")
                        .add("@param streamer a function that streams providers from the backend, not null\n")
                        .add("@param reloader a consumer that triggers a reload on the backend, not null\n")
                        .add("@return this builder instance\n")
                        .build())
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

        MethodSpec backendMethod2 = MethodSpec
                .methodBuilder("backend")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Configures a custom backend for loading providers (without reload support).\n")
                        .add("@param factory a function that creates a backend instance from a service class, not null\n")
                        .add("@param streamer a function that streams providers from the backend, not null\n")
                        .add("@return this builder instance\n")
                        .build())
                .addModifiers(PUBLIC)
                .addTypeVariable(BACKEND)
                .returns(builderName)
                .addParameter(functionOf(WILDCARD_CLASS, BACKEND), "factory")
                .addParameter(functionOf(BACKEND, iterableOf(WILDCARD)), "streamer")
                .addStatement("this.$N = ($T) factory", factoryField, functionOf(WILDCARD_CLASS, OBJECT))
                .addStatement("this.$N = ($T) streamer", streamerField, functionOf(OBJECT, iterableOf(WILDCARD)))
                .addStatement("this.$N = ignore -> {}", reloaderField)
                .addStatement("return this")
                .build();

        MethodSpec.Builder buildMethod = MethodSpec
                .methodBuilder("build")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Builds a new loader instance using the configured backend.\n")
                        .add("@return a non-null loader instance\n")
                        .build())
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
                .addMethod(backendMethod1)
                .addMethod(backendMethod2)
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
                .add("<li>Batch type: $L</li>\n", definition.getBatch().map(b -> b.getType().toString()).orElse("null"))
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
        LoadId id = ids.get(0);
        String idCall = id.getFormatMethodName().isEmpty()
                ? "o.$L()"
                : "o.$L()." + id.getFormatMethodName() + "()";
        return CodeBlock.of("o -> $N.matcher(" + idCall + ").matches()", field, id.getMethod().getSimpleName());
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

    private CodeBlock getGetDescription() {
        switch (definition.getQuantifier()) {
            case OPTIONAL:
                return CodeBlock.of(
                        "Gets an optional $L instance.\n" +
                        "<p>Returns the first available provider after applying filters and sorters, or empty if none is found.\n" +
                        "@return a non-null optional $L instance\n",
                        toJavadocLink(definition.getServiceType()), toJavadocLink(definition.getServiceType()));
            case SINGLE:
                return definition.getFallback().isPresent()
                        ? CodeBlock.of(
                                "Gets a $L instance.\n" +
                                "<p>Returns the first available provider after applying filters and sorters, or the fallback if none is found.\n" +
                                "@return a non-null $L instance\n",
                                toJavadocLink(definition.getServiceType()), toJavadocLink(definition.getServiceType()))
                        : CodeBlock.of(
                                "Gets a $L instance.\n" +
                                "<p>Returns the first available provider after applying filters and sorters.\n" +
                                "@return a non-null $L instance\n" +
                                "@throws $T if no provider is available\n",
                                toJavadocLink(definition.getServiceType()), toJavadocLink(definition.getServiceType()), IllegalStateException.class);
            case MULTIPLE:
                return CodeBlock.of(
                        "Gets a list of $L instances.\n" +
                        "<p>Returns all available providers after applying filters and sorters.\n" +
                        "@return a non-null unmodifiable list of $L instances\n",
                        toJavadocLink(definition.getServiceType()), toJavadocLink(definition.getServiceType()));
            default:
                throw new Unreachable();
        }
    }

    private MethodSpec newGetByIdMethod(FieldSpec filterFieldOrNull) {
        ClassName serviceType = definition.getServiceType();
        LoadId id = ids.get(0);
        String idMethodName = id.getMethodName();
        String idExpression = id.getFormatMethodName().isEmpty()
                ? "o." + idMethodName + "()"
                : "o." + idMethodName + "()." + id.getFormatMethodName() + "()";

        CodeBlock.Builder body = CodeBlock.builder();
        body.add("return stream()");
        if (filterFieldOrNull != null) body.add(NEW_LINE).add(".filter($L)", filterFieldOrNull.name);
        body.add(NEW_LINE).add(".filter(o -> $L.equals(id))", idExpression);
        body.add(NEW_LINE).add(".findFirst()");

        return MethodSpec
                .methodBuilder("getById")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Gets an optional $L instance by ID.\n", toJavadocLink(serviceType))
                        .add("<p>Returns the first available provider whose ID equals the given value, after applying filters.\n")
                        .add("@param id the ID to look up, not null\n")
                        .add("@return a non-null optional $L instance\n", toJavadocLink(serviceType))
                        .build())
                .addModifiers(PUBLIC)
                .returns(TypeNames.typeOf(Optional.class, serviceType))
                .addParameter(CharSequence.class, "id")
                .addStatement(body.build())
                .build();
    }

    private MethodSpec newLoadByIdMethod() {
        ClassName serviceType = definition.getServiceType();
        CodeBlock mainStatement = CodeBlock.of("builder().build().getById(id)");

        return MethodSpec
                .methodBuilder("loadById")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Gets an optional $L instance by ID.\n", toJavadocLink(serviceType))
                        .add("<p>Returns the first available provider whose ID equals the given value.\n")
                        .add("<br>This is equivalent to the following code: <code>$L</code>\n", mainStatement)
                        .add("<br>Therefore, the returned value might be different at each call.\n")
                        .add("@param id the ID to look up, not null\n")
                        .add("@return a non-null optional $L instance\n", toJavadocLink(serviceType))
                        .build())
                .addModifiers(PUBLIC, STATIC)
                .returns(TypeNames.typeOf(Optional.class, serviceType))
                .addParameter(CharSequence.class, "id")
                .addStatement("return $L", mainStatement)
                .build();
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

    private static CodeBlock getBatchMapper(BatchDefinition batchDefinition) {
        String methodName = batchDefinition.getMethodName().orElseThrow(Unreachable::new);
        switch (batchDefinition.getMethodReturnKind().orElseThrow(Unreachable::new)) {
            case STREAM:
                return CodeBlock.of("o -> o.$L()", methodName);
            case COLLECTION:
                return CodeBlock.of("o -> o.$L().stream()", methodName);
            case ITERABLE:
                return CodeBlock.of("o -> $T.stream(o.$L().spliterator(), false)", StreamSupport.class, methodName);
            case ITERATOR:
                return CodeBlock.of("o -> $T.stream($T.spliteratorUnknownSize(o.$L(), 0), false)", StreamSupport.class, Spliterators.class, methodName);
            case ARRAY:
                return CodeBlock.of("o -> $T.stream(o.$L())", Arrays.class, methodName);
            default:
                throw new Unreachable();
        }
    }

    private static final CodeBlock NEW_LINE = CodeBlock.of("\n");
}
