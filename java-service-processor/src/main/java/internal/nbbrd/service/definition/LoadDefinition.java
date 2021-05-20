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

    Quantifier quantifier;
    Lifecycle lifecycle;
    ClassName serviceType;
    Optional<TypeInstantiator> fallback;
    Optional<TypeWrapper> wrapper;
    Optional<TypeInstantiator> preprocessor;
    String loaderName;
    Optional<TypeInstantiator> backend;
    Optional<TypeInstantiator> cleaner;

    public ClassName resolveLoaderName() {
        return resolveLoaderName(loaderName, serviceType);
    }

    // visible for testing
    static ClassName resolveLoaderName(String loaderName, ClassName serviceType) {
        if (!loaderName.isEmpty()) {
            return ClassName.bestGuess(loaderName);
        }
        ClassName top = serviceType.topLevelClassName();
        ClassName topLoader = ClassName.get(top.packageName(), top.simpleName() + "Loader");
        if (top.equals(serviceType)) {
            return topLoader;
        }
        return topLoader.nestedClass(serviceType.simpleName());
    }

    static TypeMirror getPreprocessorType(ExtEnvironment env, TypeMirror service) {
        Types types = env.getTypeUtils();
        TypeMirror streamOf = types.getDeclaredType(env.asTypeElement(Stream.class), service);
        return types.getDeclaredType(env.asTypeElement(UnaryOperator.class), streamOf);
    }

    static TypeMirror getBackendType(ExtEnvironment env, TypeMirror service) {
        Types types = env.getTypeUtils();
        TypeMirror classOf = types.getDeclaredType(env.asTypeElement(Class.class));
        TypeMirror iterableOf = types.getDeclaredType(env.asTypeElement(Iterable.class));
        TypeMirror extendsIterableOf = types.getWildcardType(iterableOf, null);
        return types.getDeclaredType(env.asTypeElement(Function.class), classOf, extendsIterableOf);
    }

    static TypeMirror getCleanerType(ExtEnvironment env, TypeMirror service) {
        Types types = env.getTypeUtils();
        TypeMirror iterableOf = types.getDeclaredType(env.asTypeElement(Iterable.class));
        TypeMirror extendsIterableOf = types.getWildcardType(iterableOf, null);
        return types.getDeclaredType(env.asTypeElement(Consumer.class), extendsIterableOf);
    }
}
