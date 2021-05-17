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
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import nbbrd.service.Quantifier;

/**
 *
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

    static DeclaredType getPreprocessorType(ExtEnvironment env, TypeElement service) {
        DeclaredType streamOf = env.getTypeUtils().getDeclaredType(env.asTypeElement(Stream.class), service.asType());
        return env.getTypeUtils().getDeclaredType(env.asTypeElement(UnaryOperator.class), streamOf);
    }
}
