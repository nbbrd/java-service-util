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
package internal.nbbrd.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
final class TypeFactory {

    enum Kind {
        CONSTRUCTOR, STATIC_METHOD;
    }

    private Kind kind;
    private Element element;

    static List<TypeFactory> of(Types util, TypeElement service, TypeElement provider) {
        List<TypeFactory> result = new ArrayList<>();
        getPublicNoArgumentConstructor(provider).map(o -> new TypeFactory(Kind.CONSTRUCTOR, o)).forEach(result::add);
        getStaticFactoryMethods(util, service, provider).map(o -> new TypeFactory(Kind.STATIC_METHOD, o)).forEach(result::add);
        return result;
    }

    static Stream<ExecutableElement> getPublicNoArgumentConstructor(TypeElement type) {
        return ElementFilter
                .constructorsIn(type.getEnclosedElements())
                .stream()
                .filter(TypeFactory::isNoArgPublicMethod);
    }

    static boolean isNoArgPublicMethod(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.PUBLIC) && method.getParameters().isEmpty();
    }

    static Stream<ExecutableElement> getStaticFactoryMethods(Types util, TypeElement service, TypeElement provider) {
        return ElementFilter
                .methodsIn(provider.getEnclosedElements())
                .stream()
                .filter(TypeFactory::isNoArgPublicMethod)
                .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> util.isSubtype(method.getReturnType(), service.asType()));
    }

}
