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
import internal.nbbrd.service.ExtEnvironment;
import nbbrd.service.Quantifier;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder
class LoadDefinition {

    @lombok.NonNull
    Quantifier quantifier;

    @lombok.NonNull
    Lifecycle lifecycle;

    @lombok.NonNull
    ClassName serviceType;

    @lombok.NonNull
    Optional<TypeInstantiator> fallback;

    @lombok.NonNull
    Optional<TypeWrapper> wrapper;

    @lombok.NonNull
    Optional<TypeInstantiator> preprocessor;

    @lombok.NonNull
    String loaderName;

    @lombok.NonNull
    Optional<TypeInstantiator> backend;

    @lombok.NonNull
    Optional<TypeInstantiator> cleaner;

    boolean batch;

    @lombok.NonNull
    String batchName;

    public @NonNull ClassName resolveLoaderName() {
        return resolveName(loaderName, serviceType, "Loader");
    }

    public @NonNull ClassName resolveBatchName() {
        return resolveName(batchName, serviceType, "Batch");
    }

    // visible for testing
    static ClassName resolveName(String fullyQualifiedName, ClassName serviceType, String defaultSuffix) {
        if (!fullyQualifiedName.isEmpty()) {
            return ClassName.bestGuess(fullyQualifiedName);
        }
        ClassName top = serviceType.topLevelClassName();
        ClassName topLoader = ClassName.get(top.packageName(), top.simpleName() + defaultSuffix);
        if (top.equals(serviceType)) {
            return topLoader;
        }
        return topLoader.nestedClass(serviceType.simpleName());
    }

    static @NonNull TypeMirror getPreprocessorType(@NonNull ExtEnvironment env, @NonNull TypeMirror service) {
        Types types = env.getTypeUtils();
        TypeMirror streamOf = types.getDeclaredType(env.asTypeElement(Stream.class), service);
        return types.getDeclaredType(env.asTypeElement(UnaryOperator.class), streamOf);
    }

    static @NonNull TypeMirror getBackendType(@NonNull ExtEnvironment env, @NonNull TypeMirror service) {
        Types types = env.getTypeUtils();
        TypeMirror classOf = types.getDeclaredType(env.asTypeElement(Class.class));
        TypeMirror iterableOf = types.getDeclaredType(env.asTypeElement(Iterable.class));
        TypeMirror extendsIterableOf = types.getWildcardType(iterableOf, null);
        return types.getDeclaredType(env.asTypeElement(Function.class), classOf, extendsIterableOf);
    }

    static @NonNull TypeMirror getCleanerType(@NonNull ExtEnvironment env, @NonNull TypeMirror service) {
        Types types = env.getTypeUtils();
        TypeMirror iterableOf = types.getDeclaredType(env.asTypeElement(Iterable.class));
        TypeMirror extendsIterableOf = types.getWildcardType(iterableOf, null);
        return types.getDeclaredType(env.asTypeElement(Consumer.class), extendsIterableOf);
    }
}
