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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
public class Instantiator {

    public enum Kind {
        CONSTRUCTOR {
            @Override
            public Stream<Element> parse(Types util, TypeElement service, TypeElement provider) {
                return ElementFilter
                        .constructorsIn(provider.getEnclosedElements())
                        .stream()
                        .filter(Instantiator::isNoArgPublicMethod)
                        .map(Element.class::cast);
            }
        },
        STATIC_METHOD {
            @Override
            public Stream<Element> parse(Types util, TypeElement service, TypeElement provider) {
                return ElementFilter
                        .methodsIn(provider.getEnclosedElements())
                        .stream()
                        .filter(Instantiator::isNoArgPublicMethod)
                        .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                        .filter(method -> util.isAssignable(method.getReturnType(), service.asType()))
                        .map(Element.class::cast);
            }
        },
        ENUM_FIELD {
            @Override
            public Stream<Element> parse(Types util, TypeElement service, TypeElement provider) {
                return provider.getKind() == ElementKind.ENUM
                        ? ElementFilter
                                .fieldsIn(provider.getEnclosedElements())
                                .stream()
                                .filter(field -> field.getKind().equals(ElementKind.ENUM_CONSTANT))
                                .map(Element.class::cast)
                        : Stream.empty();
            }
        },
        STATIC_FIELD {
            @Override
            public Stream<Element> parse(Types util, TypeElement service, TypeElement provider) {
                return provider.getKind() != ElementKind.ENUM
                        ? ElementFilter
                                .fieldsIn(provider.getEnclosedElements())
                                .stream()
                                .filter(field -> field.getKind().equals(ElementKind.FIELD))
                                .filter(field -> field.getModifiers().containsAll(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)))
                                .filter(field -> util.isSubtype(field.asType(), service.asType()))
                                .map(Element.class::cast)
                        : Stream.empty();
            }
        };

        public abstract @NonNull Stream<Element> parse(@NonNull Types util, @NonNull TypeElement service, @NonNull TypeElement provider);
    }

    @lombok.NonNull
    Kind kind;

    @lombok.NonNull
    Element element;

    public static @NonNull List<Instantiator> allOf(@NonNull Types util, @NonNull TypeElement service, @NonNull TypeElement provider) {
        return Stream.of(Kind.values())
                .flatMap(kind -> kind.parse(util, service, provider).map(element -> new Instantiator(kind, element)))
                .collect(Collectors.toList());
    }

    private static boolean isNoArgPublicMethod(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.PUBLIC)
                && method.getParameters().isEmpty();
    }
}
