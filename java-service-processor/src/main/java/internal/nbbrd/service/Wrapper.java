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

import java.util.List;
import java.util.stream.Collectors;
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
public final class Wrapper {

    public enum Kind {
        CONSTRUCTOR {
            @Override
            public Stream<Element> parse(Types util, TypeElement service, TypeElement provider) {
                return ElementFilter
                        .constructorsIn(provider.getEnclosedElements())
                        .stream()
                        .filter(method -> isOneArgPublicMethod(util, method, service))
                        .map(Element.class::cast);
            }
        }, STATIC_METHOD {
            @Override
            public Stream<Element> parse(Types util, TypeElement service, TypeElement provider) {
                return ElementFilter
                        .methodsIn(provider.getEnclosedElements())
                        .stream()
                        .filter(method -> isOneArgPublicMethod(util, method, service))
                        .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                        .filter(method -> util.isSubtype(method.getReturnType(), service.asType()))
                        .map(Element.class::cast);
            }
        };

        public abstract Stream<Element> parse(Types util, TypeElement service, TypeElement provider);
    }

    private Kind kind;
    private Element element;

    public static List<Wrapper> allOf(Types util, TypeElement service, TypeElement provider) {
        return Stream.of(Kind.values())
                .flatMap(kind -> kind.parse(util, service, provider).map(element -> new Wrapper(kind, element)))
                .collect(Collectors.toList());
    }

    private static boolean isOneArgPublicMethod(Types util, ExecutableElement method, TypeElement service) {
        return method.getModifiers().contains(Modifier.PUBLIC)
                && method.getParameters().size() == 1
                && util.isSameType(method.getParameters().get(0).asType(), service.asType());
    }
}
