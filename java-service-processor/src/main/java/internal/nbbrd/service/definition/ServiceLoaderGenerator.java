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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import internal.nbbrd.service.ProcessorUtil;
import internal.nbbrd.service.InstanceFactory;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder
class ServiceLoaderGenerator {

    static ServiceLoaderGenerator of(TypeElement serviceType) {
        return of(serviceType.getAnnotation(ServiceDefinition.class), ClassName.get(serviceType));
    }

    static ServiceLoaderGenerator of(ServiceDefinition definition, ClassName serviceType) {
        return ServiceLoaderGenerator
                .builder()
                .quantifier(definition.quantifier())
                .loaderKind(LoaderKind.of(definition.mutability(), definition.singleton()))
                .serviceType(serviceType)
                .fallbackType(nonNull(definition::fallback))
                .preprocessorType(nonNull(definition::preprocessor))
                .loaderName(definition.loaderName())
                .build();
    }

    private Quantifier quantifier;
    private LoaderKind loaderKind;
    private ClassName serviceType;
    private Optional<TypeMirror> fallbackType;
    private Optional<TypeMirror> preprocessorType;
    private String loaderName;

    public TypeSpec generate(String className, Function<TypeMirror, InstanceFactory> toFactory) {
        TypeName quantifierType = getQuantifierType();

        FieldSpec sourceField = newSourceField();
        MethodSpec doLoadMethod = newDoLoadMethod(sourceField, quantifierType, toFactory);
        FieldSpec resourceField = newResourceField(doLoadMethod, quantifierType);
        MethodSpec getMethod = newGetMethod(resourceField, quantifierType);

        TypeSpec.Builder result = TypeSpec.classBuilder(className)
                .addJavadoc(getMainJavadoc())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(sourceField)
                .addMethod(doLoadMethod)
                .addField(resourceField)
                .addMethod(getMethod);

        if (loaderKind.isSingleton()) {
            result.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        }

        if (loaderKind.isModifiable()) {
            result.addMethod(newSetMethod(resourceField, quantifierType));
            result.addMethod(newReloadMethod(sourceField, doLoadMethod));
        }

        if (loaderKind == LoaderKind.IMMUTABLE) {
            result.addMethod(newLoadMethod(className, quantifierType, getMethod));
        }

        return result.build();
    }

    private CodeBlock getMainJavadoc() {
        return CodeBlock
                .builder()
                .add("Custom service loader for $L.\n", toJavadocLink(serviceType))
                .add("<br>This class $L thread-safe.\n", loaderKind.isThreadSafe() ? "is" : "is not")
                .add("<p>Properties:\n")
                .add("<li>Quantifier: $L\n", quantifier)
                .add("<li>Fallback: $L\n", toJavadocLink(fallbackType))
                .add("<li>Preprocessor: $L\n", toJavadocLink(preprocessorType))
                .add("<li>Mutability: $L\n", loaderKind.toMutability())
                .add("<li>Singleton: $L\n", loaderKind.isSingleton())
                .add("<li>Name: $L\n", loaderName.isEmpty() ? "null" : loaderName)
                .build();
    }

    private MethodSpec newDoLoadMethod(FieldSpec sourceField, TypeName quantifierType, Function<TypeMirror, InstanceFactory> toFactory) {
        return MethodSpec.methodBuilder("doLoad")
                .addModifiers(Modifier.PRIVATE)
                .addModifiers(getSingletonModifiers())
                .returns(quantifierType)
                .addExceptions(getQuantifierException())
                .addStatement(CodeBlock
                        .builder()
                        .add("return ")
                        .add(getPreprocessorCode(sourceField, toFactory))
                        .add(getQuantifierCode(toFactory))
                        .build())
                .build();
    }

    private CodeBlock getPreprocessorCode(FieldSpec sourceField, Function<TypeMirror, InstanceFactory> toFactory) {
        return preprocessorType.isPresent()
                ? CodeBlock.of("$L\n.apply($T.stream($N.spliterator(), false))", getFactoryCode(preprocessorType.get(), toFactory), StreamSupport.class, sourceField)
                : CodeBlock.of("$T.stream($N.spliterator(), false)", StreamSupport.class, sourceField);
    }

    private CodeBlock getQuantifierCode(Function<TypeMirror, InstanceFactory> toFactory) {
        switch (quantifier) {
            case OPTIONAL:
                return CodeBlock.of("\n.findFirst()");
            case SINGLE:
                return fallbackType.isPresent()
                        ? CodeBlock.of("\n.findFirst()\n.orElseGet(() -> $L)", getFactoryCode(fallbackType.get(), toFactory))
                        : CodeBlock.of("\n.findFirst()\n.orElseThrow(() -> new $T(\"Missing mandatory provider of $T\"))", IllegalStateException.class, serviceType);
            case MULTIPLE:
                return CodeBlock.of("\n.collect($T.collectingAndThen($T.toList(), $T::unmodifiableList))", Collectors.class, Collectors.class, Collections.class);
            default:
                throw new RuntimeException();
        }
    }

    private CodeBlock getFactoryCode(TypeMirror type, Function<TypeMirror, InstanceFactory> toFactory) {
        InstanceFactory factory = toFactory.apply(type);
        switch (factory.getKind()) {
            case CONSTRUCTOR:
                return CodeBlock.of("new $T()", type);
            case STATIC_METHOD:
                return CodeBlock.of("$T.$L()", type, factory.getElement().getSimpleName());
            case ENUM_FIELD:
            case STATIC_FIELD:
                return CodeBlock.of("$T.$L", type, factory.getElement().getSimpleName());
            default:
                throw new RuntimeException();
        }
    }

    private TypeName getQuantifierType() {
        switch (quantifier) {
            case OPTIONAL:
                return typeOf(Optional.class, serviceType);
            case SINGLE:
                return serviceType;
            case MULTIPLE:
                return typeOf(List.class, serviceType);
            default:
                throw new RuntimeException();
        }
    }

    private List<TypeName> getQuantifierException() {
        return quantifier == Quantifier.SINGLE && !fallbackType.isPresent()
                ? Collections.singletonList(ClassName.get(IllegalStateException.class))
                : Collections.emptyList();
    }

    private FieldSpec newSourceField() {
        return FieldSpec.builder(typeOf(ServiceLoader.class, serviceType), fieldName("source"))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .addModifiers(getSingletonModifiers())
                .initializer("$T.load($T.class)", ServiceLoader.class, serviceType)
                .build();
    }

    private FieldSpec newResourceField(MethodSpec loader, TypeName quantifierType) {
        return getResourceFieldBuilder(quantifierType)
                .addModifiers(getSingletonModifiers())
                .initializer(getResourceInitializer(loader))
                .build();
    }

    private FieldSpec.Builder getResourceFieldBuilder(TypeName quantifierType) {
        String name = fieldName("resource");
        switch (loaderKind) {
            case IMMUTABLE:
            case CONSTANT:
                return FieldSpec.builder(quantifierType, name, Modifier.PRIVATE, Modifier.FINAL);
            case MUTABLE:
            case UNSAFE_MUTABLE:
                return FieldSpec.builder(quantifierType, name, Modifier.PRIVATE);
            case CONCURRENT:
            case ATOMIC:
                return FieldSpec.builder(typeOf(AtomicReference.class, quantifierType), name, Modifier.PRIVATE, Modifier.FINAL);
            default:
                throw new RuntimeException();
        }
    }

    private CodeBlock getResourceInitializer(MethodSpec loader) {
        return loaderKind.isAtomicReference()
                ? CodeBlock.of("new $T<>($N())", ClassName.get(AtomicReference.class), loader)
                : CodeBlock.of("$N()", loader);
    }

    private MethodSpec newGetMethod(FieldSpec resourceField, TypeName quantifierType) {
        return MethodSpec.methodBuilder("get")
                .addJavadoc(CodeBlock
                        .builder()
                        .add(getGetDescription())
                        .add(getThreadSafetyComment())
                        .add("@return the current non-null value\n")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(getSingletonModifiers())
                .returns(quantifierType)
                .addStatement(getGetterStatement(resourceField))
                .build();
    }

    private CodeBlock getGetDescription() {
        switch (quantifier) {
            case OPTIONAL:
                return CodeBlock.of("Gets an optional $L instance.\n", toJavadocLink(serviceType));
            case SINGLE:
                return CodeBlock.of("Gets a $L instance.\n", toJavadocLink(serviceType));
            case MULTIPLE:
                return CodeBlock.of("Gets a list of $L instances.\n", toJavadocLink(serviceType));
            default:
                throw new RuntimeException();
        }
    }

    private CodeBlock getGetterStatement(FieldSpec resourceField) {
        return loaderKind.isAtomicReference()
                ? CodeBlock.of("return $N.get()", resourceField)
                : CodeBlock.of("return $N", resourceField);
    }

    private MethodSpec newSetMethod(FieldSpec resourceField, TypeName quantifierType) {
        return MethodSpec.methodBuilder("set")
                .addJavadoc(CodeBlock
                        .builder()
                        .add(getSetDescription())
                        .add(getThreadSafetyComment())
                        .add("@param newValue new non-null value\n")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(getSingletonModifiers())
                .addParameter(quantifierType, "newValue")
                .addStatement(getSetterStatement(resourceField))
                .build();
    }

    private CodeBlock getSetDescription() {
        switch (quantifier) {
            case OPTIONAL:
                return CodeBlock.of("Sets an optional $L instance.\n", toJavadocLink(serviceType));
            case SINGLE:
                return CodeBlock.of("Sets a $L instance.\n", toJavadocLink(serviceType));
            case MULTIPLE:
                return CodeBlock.of("Sets a list of $L instances.\n", toJavadocLink(serviceType));
            default:
                throw new RuntimeException();
        }
    }

    private CodeBlock getSetterStatement(FieldSpec resourceField) {
        return loaderKind.isAtomicReference()
                ? CodeBlock.of("$N.set($T.requireNonNull(newValue))", resourceField, Objects.class)
                : CodeBlock.of("$N = $T.requireNonNull(newValue)", resourceField, Objects.class);
    }

    private MethodSpec newReloadMethod(FieldSpec sourceField, MethodSpec loader) {
        MethodSpec.Builder result = MethodSpec.methodBuilder("reload")
                .addJavadoc(CodeBlock
                        .builder()
                        .add("Reloads the content by clearing the cache and fetching available providers.\n")
                        .add(getThreadSafetyComment())
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(getSingletonModifiers())
                .addExceptions(getQuantifierException());

        if (loaderKind.isAtomicReference()) {
            result.beginControlFlow("synchronized($N)", sourceField);
        }

        result.addStatement("$N.reload()", sourceField);
        result.addStatement("set($N())", loader);

        if (loaderKind.isAtomicReference()) {
            result.endControlFlow();
        }

        return result.build();
    }

    private MethodSpec newLoadMethod(String className, TypeName quantifierType, MethodSpec getter) {
        CodeBlock mainStatement = CodeBlock.of("new $L().$N()", className, getter);

        MethodSpec.Builder result = MethodSpec.methodBuilder("load")
                .addJavadoc(CodeBlock
                        .builder()
                        .add(getGetDescription())
                        .add("<br>This is equivalent to the following code: <code>$L</code>\n", mainStatement)
                        .add("<br>Therefore, the returned value might be different at each call.\n")
                        .add(getThreadSafetyComment())
                        .add("@return a non-null value\n")
                        .build())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(quantifierType)
                .addExceptions(getQuantifierException());

        result.addStatement("return $L", mainStatement);

        return result.build();
    }

    private Modifier[] getSingletonModifiers() {
        return loaderKind.isSingleton() ? SINGLETON_MODIFIER : NO_MODIFIER;
    }

    private String fieldName(String name) {
        return loaderKind.isSingleton() ? name.toUpperCase() : name;
    }

    private CodeBlock getThreadSafetyComment() {
        return loaderKind.isThreadSafe()
                ? CodeBlock.of("<br>This method is thread-safe.\n")
                : CodeBlock.of("<br>This method is not thread-safe.\n");
    }

    private static Optional<TypeMirror> nonNull(Supplier<Class<?>> type) {
        return Optional.of(ProcessorUtil.extractResultType(type)).filter(ServiceLoaderGenerator::isNonNullValue);
    }

    private static boolean isNonNullValue(TypeMirror type) {
        switch (type.toString()) {
            case "nbbrd.service.ServiceDefinition.NoProcessing":
            case "java.lang.Void":
                return false;
            default:
                return true;
        }
    }

    private static ParameterizedTypeName typeOf(Class<?> rawType, TypeName typeArgument) {
        return ParameterizedTypeName.get(ClassName.get(rawType), typeArgument);
    }

    private static String toJavadocLink(ClassName type) {
        return "{@link " + type + "}";
    }

    private static String toJavadocLink(Optional<TypeMirror> type) {
        return type.map(o -> "{@link " + o + "}").orElse("null");
    }

    private static final Modifier[] NO_MODIFIER = new Modifier[0];
    private static final Modifier[] SINGLETON_MODIFIER = new Modifier[]{Modifier.STATIC};
}