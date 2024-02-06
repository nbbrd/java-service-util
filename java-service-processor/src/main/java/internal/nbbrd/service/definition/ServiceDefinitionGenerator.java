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
import internal.nbbrd.service.*;
import nbbrd.service.Quantifier;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static internal.nbbrd.service.Blocks.*;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.*;

/**
 * @author Philippe Charles
 */
@lombok.Value
class ServiceDefinitionGenerator {

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

    public boolean hasCustomBatchName() {
        return !definition.getBatchName().isEmpty();
    }

    public TypeSpec generateLoader(boolean nested) {
        String className = definition.resolveLoaderName().simpleName();

        TypeName quantifierType = getQuantifierType();

        FieldSpec sourceField = getSourceField();
        Optional<FieldSpec> batchField = getBatchField();

        Optional<FieldSpec> idPatternField = getIdPatternField();
        Optional<FieldSpec> filterField = getFilterField(idPatternField);
        Optional<FieldSpec> sorterField = getSorterField();

        CodeBlock rawStreamCode = getRawStreamCode(sourceField, batchField);

        MethodSpec doLoadMethod = getDoLoadMethod(rawStreamCode, filterField, sorterField, quantifierType);
        FieldSpec resourceField = getResourceField(doLoadMethod, quantifierType);
        MethodSpec getMethod = getGetMethod(resourceField, quantifierType);

        TypeSpec.Builder result = TypeSpec
                .classBuilder(className)
                .addJavadoc(getMainJavadoc())
                .addModifiers(PUBLIC, FINAL)
                .addField(sourceField);

        idPatternField.ifPresent(result::addField);
        batchField.ifPresent(result::addField);
        filterField.ifPresent(result::addField);
        sorterField.ifPresent(result::addField);

        result
                .addMethod(doLoadMethod)
                .addField(resourceField)
                .addMethod(getMethod);

        if (nested) {
            result.addModifiers(STATIC).build();
        }

        if (definition.getLifecycle().isSingleton()) {
            result.addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());
        }

        if (definition.getLifecycle().isModifiable()) {
            FieldSpec cleanerField = newCleanerField();
            result.addField(cleanerField);
            result.addMethod(newSetMethod(resourceField, quantifierType));
            result.addMethod(newReloadMethod(sourceField, batchField, cleanerField, doLoadMethod));
            result.addMethod(newResetMethod(sourceField, doLoadMethod));
        }

        if (definition.getLifecycle() == Lifecycle.IMMUTABLE) {
            result.addMethod(newLoadMethod(className, quantifierType, getMethod));
        }

        return result.build();
    }

    public Optional<TypeSpec> generateBatch(boolean nested) {
        if (!definition.isBatch()) {
            return Optional.empty();
        }

        String className = definition.resolveBatchName().simpleName();

        TypeSpec.Builder result = TypeSpec
                .interfaceBuilder(className)
                .addModifiers(PUBLIC)
                .addMethod(MethodSpec
                        .methodBuilder("getProviders")
                        .addModifiers(PUBLIC, ABSTRACT)
                        .returns(typeOf(Stream.class, definition.getServiceType()))
                        .build());

        if (nested) {
            result.addModifiers(STATIC).build();
        }

        return Optional.of(result.build());
    }

    private CodeBlock getMainJavadoc() {
        return CodeBlock
                .builder()
                .add("Custom service loader for $L.\n", toJavadocLink(definition.getServiceType()))
                .add("<br>This class $L thread-safe.\n", definition.getLifecycle().isThreadSafe() ? "is" : "is not")
                .add("<p>Properties:\n")
                .add("<ul>\n")
                .add("<li>Quantifier: $L</li>\n", definition.getQuantifier())
                .add("<li>Fallback: $L</li>\n", toJavadocLink(definition.getFallback()))
                .add("<li>Preprocessing: $L</li>\n", getPreprocessingJavadoc())
                .add("<li>Mutability: $L</li>\n", definition.getLifecycle().toMutability())
                .add("<li>Singleton: $L</li>\n", definition.getLifecycle().isSingleton())
                .add("<li>Name: $L</li>\n", definition.getLoaderName().isEmpty() ? "null" : definition.getLoaderName())
                .add("<li>Backend: $L</li>\n", definition.getBackend().map(HasTypeMirror::getTypeName).orElse("null"))
                .add("<li>Cleaner: $L</li>\n", definition.getCleaner().map(HasTypeMirror::getTypeName).orElse("null"))
                .add("<li>Batch: $L</li>\n", definition.isBatch())
                .add("<li>Batch name: $L</li>\n", definition.getBatchName().isEmpty() ? "null" : definition.getBatchName())
                .add("</ul>\n")
                .build();
    }

    private String getPreprocessingJavadoc() {
        return definition.getPreprocessor().isPresent()
                ? getAdvancedPreprocessingJavadoc()
                : getBasicPreprocessingJavadoc();
    }

    private String getAdvancedPreprocessingJavadoc() {
        return toJavadocLink(definition.getPreprocessor());
    }

    private String getBasicPreprocessingJavadoc() {
        if (definition.getWrapper().isPresent() || !filters.isEmpty() || !sorters.isEmpty()) {
            return "wrapper: " + definition.getWrapper().map(HasTypeMirror::getTypeName).orElse("none")
                    + " filters:" + filters.stream().collect(toMethodNames())
                    + " sorters:" + sorters.stream().collect(toMethodNames());
        }
        return "null";
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private MethodSpec getDoLoadMethod(
            CodeBlock rawStreamCode,
            Optional<FieldSpec> filterField,
            Optional<FieldSpec> sorterField,
            TypeName quantifierType
    ) {
        return MethodSpec
                .methodBuilder("doLoad")
                .addModifiers(PRIVATE)
                .addModifiers(getSingletonModifiers())
                .returns(quantifierType)
                .addExceptions(getQuantifierException())
                .addStatement(CodeBlock
                        .builder()
                        .add("return ")
                        .add(getPreprocessingCode(rawStreamCode, filterField, sorterField))
                        .add(getQuantifierCode())
                        .build())
                .build();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private CodeBlock getPreprocessingCode(
            CodeBlock rawStreamCode,
            Optional<FieldSpec> filterField,
            Optional<FieldSpec> sorterField
    ) {
        return definition.getPreprocessor().isPresent()
                ? getAdvancedPreprocessingCode(rawStreamCode, filterField, sorterField, definition.getPreprocessor().get())
                : getBasicPreprocessingCode(rawStreamCode, filterField, sorterField);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private CodeBlock getRawStreamCode(FieldSpec sourceField, Optional<FieldSpec> batchField) {
        final CodeBlock stream = iterableToStream(sourceField);
        return batchField
                .map(field -> concatStreams(stream, getBatchStreamCode(field)))
                .orElse(stream);
    }

    private CodeBlock getBatchStreamCode(FieldSpec field) {
        return flatMapStream(iterableToStream(field), CodeBlock.of("o -> o.getProviders()"));
    }

    private CodeBlock getIdPredicateCode(FieldSpec field) {
        return CodeBlock.of("o -> $N.matcher(o.$L()).matches()", field, ids.get(0).getMethod().getSimpleName());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private CodeBlock getAdvancedPreprocessingCode(
            CodeBlock streamBlock,
            Optional<FieldSpec> filterField,
            Optional<FieldSpec> sorterField,
            TypeInstantiator preprocessor
    ) {
        CodeBlock result = CodeBlock.of("$L\n.apply($L)", getInstantiatorCode(preprocessor), streamBlock);
        return getBasicPreprocessingCode(result, filterField, sorterField);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private CodeBlock getBasicPreprocessingCode(
            CodeBlock streamBlock,
            Optional<FieldSpec> filterField,
            Optional<FieldSpec> sorterField
    ) {
        CodeBlock.Builder result = CodeBlock.builder();
        result.add(streamBlock);
        if (definition.getWrapper().isPresent()) {
            result.add(NEW_LINE).add(".map($L)", getWrapperCode(definition.getWrapper().get()));
            result.add(NEW_LINE).add(".map($T.class::cast)", definition.getServiceType());
        }
        filterField.ifPresent(field -> result.add(NEW_LINE).add(".filter($L)", field.name));
        sorterField.ifPresent(field -> result.add(NEW_LINE).add(".sorted($L)", field.name));
        return result.build();
    }

    private CodeBlock getFiltersCode(Optional<FieldSpec> idPatternField) {
        List<CodeBlock> blocks = new ArrayList<>();
        idPatternField.map(this::getIdPredicateCode).ifPresent(blocks::add);
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
        result.add(casting(typeOf(Predicate.class, definition.getServiceType()), first));
        while (iterator.hasNext()) {
            result.add(".and($L)", iterator.next());
        }
        return result.build();
    }

    private CodeBlock getFilterCode(LoadFilter filter) {
        CodeBlock result = CodeBlock.of("$T::$L", filter.getServiceType().orElseThrow(Unreachable::new), filter.getMethodName());
        return filter.isNegate()
                ? CodeBlock.of("$L.negate()", casting(typeOf(Predicate.class, definition.getServiceType()), result))
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
        result.add(casting(typeOf(Comparator.class, definition.getServiceType()), first));
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
        switch (sorter.getKeyType().get()) {
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

    private CodeBlock getWrapperCode(TypeWrapper instance) {
        Wrapper wrapper = instance.select().orElseThrow(RuntimeException::new);
        switch (wrapper.getKind()) {
            case CONSTRUCTOR:
                return CodeBlock.of("$T::new", instance.getType());
            case STATIC_METHOD:
                return CodeBlock.of("$T::$L", instance.getType(), wrapper.getElement().getSimpleName());
            default:
                throw new Unreachable();
        }
    }

    private TypeName getQuantifierType() {
        switch (definition.getQuantifier()) {
            case OPTIONAL:
                return typeOf(Optional.class, definition.getServiceType());
            case SINGLE:
                return definition.getServiceType();
            case MULTIPLE:
                return typeOf(List.class, definition.getServiceType());
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
                .builder(typeOf(Iterable.class, definition.getServiceType()), fieldName("source"))
                .addModifiers(PRIVATE, FINAL)
                .addModifiers(getSingletonModifiers())
                .initializer("$L", getBackendInitCode(definition.getServiceType()))
                .build();
    }

    private Optional<FieldSpec> getBatchField() {
        if (definition.isBatch()) {
            return Optional.of(FieldSpec
                    .builder(typeOf(Iterable.class, definition.resolveBatchName()), fieldName("batch"))
                    .addModifiers(PRIVATE, FINAL)
                    .addModifiers(getSingletonModifiers())
                    .initializer("$L", getBackendInitCode(definition.resolveBatchName()))
                    .build());
        }
        if (definition.getBatchType().isPresent()) {
            ClassName batchTypeName = ClassName.bestGuess(definition.getBatchType().get().toString());
            return Optional.of(FieldSpec
                    .builder(typeOf(Iterable.class, batchTypeName), fieldName("batch"))
                    .addModifiers(PRIVATE, FINAL)
                    .addModifiers(getSingletonModifiers())
                    .initializer("$L", getBackendInitCode(batchTypeName))
                    .build());
        }
        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<FieldSpec> getFilterField(Optional<FieldSpec> idPatternField) {
        return !filters.isEmpty() || idPatternField.isPresent()
                ? Optional.of(FieldSpec
                .builder(typeOf(Predicate.class, definition.getServiceType()), fieldName("filter"))
                .addModifiers(PRIVATE, FINAL)
                .addModifiers(getSingletonModifiers())
                .initializer("$L", getFiltersCode(idPatternField))
                .build())
                : Optional.empty();
    }

    private Optional<FieldSpec> getSorterField() {
        return !sorters.isEmpty()
                ? Optional.of(FieldSpec
                .builder(typeOf(Comparator.class, definition.getServiceType()), fieldName("sorter"))
                .addModifiers(PRIVATE, FINAL)
                .addModifiers(getSingletonModifiers())
                .initializer("$L", getSortersCode())
                .build())
                : Optional.empty();
    }

    private CodeBlock getBackendInitCode(ClassName serviceType) {
        return definition.getBackend().isPresent()
                ? CodeBlock.of("$L.apply($T.class)", getInstantiatorCode(definition.getBackend().get()), serviceType)
                : CodeBlock.of("$T.load($T.class)", ServiceLoader.class, serviceType);
    }

    private FieldSpec newCleanerField() {
        return FieldSpec
                .builder(typeOf(Consumer.class, ClassName.get(Iterable.class)), fieldName("cleaner"))
                .addModifiers(PRIVATE, FINAL)
                .addModifiers(getSingletonModifiers())
                .initializer("$L", getCleanerInitCode())
                .build();
    }

    private CodeBlock getCleanerInitCode() {
        return definition.getCleaner().isPresent()
                ? CodeBlock.of("$L", getInstantiatorCode(definition.getCleaner().get()))
                : CodeBlock.of("loader -> (($T)loader).reload()", ServiceLoader.class);
    }

    private FieldSpec getResourceField(MethodSpec doLoadMethod, TypeName quantifierType) {
        return getResourceFieldBuilder(quantifierType)
                .addModifiers(getSingletonModifiers())
                .initializer(getResourceInitializer(doLoadMethod))
                .build();
    }

    private FieldSpec.Builder getResourceFieldBuilder(TypeName quantifierType) {
        String name = fieldName("resource");
        switch (definition.getLifecycle()) {
            case IMMUTABLE:
            case CONSTANT:
                return FieldSpec.builder(quantifierType, name, PRIVATE, FINAL);
            case MUTABLE:
            case UNSAFE_MUTABLE:
                return FieldSpec.builder(quantifierType, name, PRIVATE);
            case CONCURRENT:
            case ATOMIC:
                return FieldSpec.builder(typeOf(AtomicReference.class, quantifierType), name, PRIVATE, FINAL);
            default:
                throw new Unreachable();
        }
    }

    private CodeBlock getResourceInitializer(MethodSpec doLoadMethod) {
        return definition.getLifecycle().isAtomicReference()
                ? CodeBlock.of("new $T<>($N())", ClassName.get(AtomicReference.class), doLoadMethod)
                : CodeBlock.of("$N()", doLoadMethod);
    }

    private MethodSpec getGetMethod(FieldSpec resourceField, TypeName quantifierType) {
        return MethodSpec
                .methodBuilder("get")
                .addJavadoc(CodeBlock
                        .builder()
                        .add(getGetDescription())
                        .add(getThreadSafetyComment())
                        .add("@return the current non-null value\n")
                        .build())
                .addModifiers(PUBLIC)
                .addModifiers(getSingletonModifiers())
                .returns(quantifierType)
                .addStatement(getGetterStatement(resourceField))
                .build();
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

    private CodeBlock getGetterStatement(FieldSpec resourceField) {
        return definition.getLifecycle().isAtomicReference()
                ? CodeBlock.of("return $N.get()", resourceField)
                : CodeBlock.of("return $N", resourceField);
    }

    private MethodSpec newSetMethod(FieldSpec resourceField, TypeName quantifierType) {
        return MethodSpec
                .methodBuilder("set")
                .addJavadoc(CodeBlock
                        .builder()
                        .add(getSetDescription())
                        .add(getThreadSafetyComment())
                        .add("@param newValue new non-null value\n")
                        .build())
                .addModifiers(PUBLIC)
                .addModifiers(getSingletonModifiers())
                .addParameter(quantifierType, "newValue")
                .addStatement(getSetterStatement(resourceField))
                .build();
    }

    private CodeBlock getSetDescription() {
        switch (definition.getQuantifier()) {
            case OPTIONAL:
                return CodeBlock.of("Sets an optional $L instance.\n", toJavadocLink(definition.getServiceType()));
            case SINGLE:
                return CodeBlock.of("Sets a $L instance.\n", toJavadocLink(definition.getServiceType()));
            case MULTIPLE:
                return CodeBlock.of("Sets a list of $L instances.\n", toJavadocLink(definition.getServiceType()));
            default:
                throw new Unreachable();
        }
    }

    private CodeBlock getSetterStatement(FieldSpec resourceField) {
        return definition.getLifecycle().isAtomicReference()
                ? CodeBlock.of("$N.set($T.requireNonNull(newValue))", resourceField, Objects.class)
                : CodeBlock.of("$N = $T.requireNonNull(newValue)", resourceField, Objects.class);
    }

    private MethodSpec newReloadMethod(
            FieldSpec sourceField,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<FieldSpec> batchField,
            FieldSpec cleanerField,
            MethodSpec loaderMethod
    ) {
        MethodSpec.Builder result = MethodSpec
                .methodBuilder("reload")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Reloads the content by clearing the cache and fetching available providers.\n")
                        .add(getThreadSafetyComment())
                        .build())
                .addModifiers(PUBLIC)
                .addModifiers(getSingletonModifiers())
                .addExceptions(getQuantifierException());

        if (definition.getLifecycle().isAtomicReference()) {
            result.beginControlFlow("synchronized($N)", sourceField);
        }

        result.addStatement("$N.accept($N)", cleanerField, sourceField);
        batchField.ifPresent(fieldSpec -> result.addStatement("$N.accept($N)", cleanerField, fieldSpec));
        result.addStatement("set($N())", loaderMethod);

        if (definition.getLifecycle().isAtomicReference()) {
            result.endControlFlow();
        }

        return result.build();
    }

    private MethodSpec newResetMethod(FieldSpec sourceField, MethodSpec loaderMethod) {
        MethodSpec.Builder result = MethodSpec
                .methodBuilder("reset")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Resets the content without clearing the cache.\n")
                        .add(getThreadSafetyComment())
                        .build())
                .addModifiers(PUBLIC)
                .addModifiers(getSingletonModifiers())
                .addExceptions(getQuantifierException());

        if (definition.getLifecycle().isAtomicReference()) {
            result.beginControlFlow("synchronized($N)", sourceField);
        }

        result.addStatement("set($N())", loaderMethod);

        if (definition.getLifecycle().isAtomicReference()) {
            result.endControlFlow();
        }

        return result.build();
    }

    private MethodSpec newLoadMethod(String className, TypeName quantifierType, MethodSpec getter) {
        CodeBlock mainStatement = CodeBlock.of("new $L().$N()", className, getter);

        MethodSpec.Builder result = MethodSpec
                .methodBuilder("load")
                .addJavadoc(CodeBlock
                        .builder()
                        .add(getGetDescription())
                        .add("<br>This is equivalent to the following code: <code>$L</code>\n", mainStatement)
                        .add("<br>Therefore, the returned value might be different at each call.\n")
                        .add(getThreadSafetyComment())
                        .add("@return a non-null value\n")
                        .build())
                .addModifiers(PUBLIC, STATIC)
                .returns(quantifierType)
                .addExceptions(getQuantifierException());

        result.addStatement("return $L", mainStatement);

        return result.build();
    }


    private Optional<FieldSpec> getIdPatternField() {
        return ids.size() == 1 && !ids.get(0).getPattern().isEmpty()
                ? Optional.of(FieldSpec
                .builder(Pattern.class, fieldName("ID_PATTERN"))
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer("$T.compile(\"$N\")", Pattern.class, ids.get(0).getPattern())
                .build())
                : Optional.empty();
    }

    private Modifier[] getSingletonModifiers() {
        return definition.getLifecycle().isSingleton() ? SINGLETON_MODIFIER : NO_MODIFIER;
    }

    private String fieldName(String name) {
        return definition.getLifecycle().isSingleton() ? name.toUpperCase(Locale.ROOT) : name;
    }

    private CodeBlock getThreadSafetyComment() {
        return definition.getLifecycle().isThreadSafe()
                ? CodeBlock.of("<br>This method is thread-safe.\n")
                : CodeBlock.of("<br>This method is not thread-safe.\n");
    }

    private static ParameterizedTypeName typeOf(Class<?> rawType, TypeName typeArgument) {
        return ParameterizedTypeName.get(ClassName.get(rawType), typeArgument);
    }

    private static String toJavadocLink(ClassName type) {
        return "{@link " + type + "}";
    }

    private static String toJavadocLink(Optional<TypeInstantiator> type) {
        return type.map(o -> "{@link " + o.getType() + "}").orElse("null");
    }

    private static Collector<HasMethod, ?, String> toMethodNames() {
        return Collectors.mapping(HasMethod::getMethodName, Collectors.joining("+", "[", "]"));
    }

    private static final Modifier[] NO_MODIFIER = new Modifier[0];
    private static final Modifier[] SINGLETON_MODIFIER = new Modifier[]{STATIC};
    private static final CodeBlock NEW_LINE = CodeBlock.of("\n");
}
