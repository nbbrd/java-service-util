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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
public final class InstanceFactory {

    public enum Kind {
        CONSTRUCTOR, STATIC_METHOD, ENUM_FIELD, STATIC_FIELD;
    }

    private Kind kind;
    private Element element;

    public static List<InstanceFactory> allOf(Types util, TypeElement type) {
        return allOf(util, type, type);
    }

    public static List<InstanceFactory> allOf(Types util, TypeElement service, TypeElement provider) {
        List<InstanceFactory> result = new ArrayList<>();
        getPublicNoArgumentConstructor(provider).map(o -> new InstanceFactory(Kind.CONSTRUCTOR, o)).forEach(result::add);
        getStaticFactoryMethods(util, service, provider).map(o -> new InstanceFactory(Kind.STATIC_METHOD, o)).forEach(result::add);
        getEnumFields(util, provider).map(o -> new InstanceFactory(Kind.ENUM_FIELD, o)).forEach(result::add);
        getStaticFields(util, service, provider).map(o -> new InstanceFactory(Kind.STATIC_FIELD, o)).forEach(result::add);
        return result;
    }

    static Stream<ExecutableElement> getPublicNoArgumentConstructor(TypeElement type) {
        return ElementFilter
                .constructorsIn(type.getEnclosedElements())
                .stream()
                .filter(InstanceFactory::isNoArgPublicMethod);
    }

    static boolean isNoArgPublicMethod(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.PUBLIC) && method.getParameters().isEmpty();
    }

    static Stream<ExecutableElement> getStaticFactoryMethods(Types util, TypeElement service, TypeElement provider) {
        return ElementFilter
                .methodsIn(provider.getEnclosedElements())
                .stream()
                .filter(InstanceFactory::isNoArgPublicMethod)
                .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                .filter(method -> util.isSubtype(method.getReturnType(), service.asType()));
    }

    static Stream<VariableElement> getEnumFields(Types util, TypeElement provider) {
        return provider.getKind() == ElementKind.ENUM
                ? ElementFilter
                        .fieldsIn(provider.getEnclosedElements())
                        .stream()
                        .filter(field -> field.getKind().equals(ElementKind.ENUM_CONSTANT))
                : Stream.empty();
    }

    static Stream<VariableElement> getStaticFields(Types util, TypeElement service, TypeElement provider) {
        return provider.getKind() != ElementKind.ENUM
                ? ElementFilter
                        .fieldsIn(provider.getEnclosedElements())
                        .stream()
                        .filter(field -> field.getKind().equals(ElementKind.FIELD))
                        .filter(field -> field.getModifiers().containsAll(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)))
                        .filter(field -> util.isSubtype(field.asType(), service.asType()))
                : Stream.empty();
    }

    public static boolean canSelect(List<InstanceFactory> factories) {
        return factories.size() == 1
                || factories.stream().anyMatch(o -> o.getKind() == InstanceFactory.Kind.CONSTRUCTOR);
    }

    public static InstanceFactory select(List<InstanceFactory> factories) {
        return factories.size() == 1
                ? factories.get(0)
                : factories.stream().filter(o -> o.getKind() == InstanceFactory.Kind.CONSTRUCTOR).findFirst().orElseThrow(RuntimeException::new);
    }
}
