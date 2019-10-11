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
import internal.nbbrd.service.Instantiator;
import internal.nbbrd.service.ProcessorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import nbbrd.service.ServiceDefinition;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
final class ServiceDefinitionCollector {

    private final ProcessingEnvironment env;

    public List<DefinitionValue> collect(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<TypeElement> serviceDefinitions = new ArrayList<>();
        for (TypeElement annotation : annotations) {
            switch (annotation.getSimpleName().toString()) {
                case "ServiceDefinition":
                    roundEnv.getElementsAnnotatedWith(annotation).stream().map(TypeElement.class::cast).forEach(serviceDefinitions::add);
                    break;
            }
        }
        return serviceDefinitions
                .stream()
                .map(definitionType -> of(definitionType))
                .collect(Collectors.toList());
    }

    private DefinitionValue of(TypeElement serviceType) {
        return of(serviceType.getAnnotation(ServiceDefinition.class), ClassName.get(serviceType));
    }

    private DefinitionValue of(ServiceDefinition annotation, ClassName serviceType) {
        Types types = env.getTypeUtils();

        Optional<TypeHandler> fallback = nonNull(annotation::fallback)
                .map(type -> new TypeHandler(type, Instantiator.allOf(types, (TypeElement) types.asElement(type))));

        Optional<TypeHandler> preprocessor = nonNull(annotation::preprocessor)
                .map(type -> new TypeHandler(type, Instantiator.allOf(types, (TypeElement) types.asElement(type))));

        return DefinitionValue
                .builder()
                .quantifier(annotation.quantifier())
                .lifecycle(Lifecycle.of(annotation.mutability(), annotation.singleton()))
                .serviceType(serviceType)
                .fallback(fallback)
                .preprocessor(preprocessor)
                .loaderName(annotation.loaderName())
                .build();
    }

    private Optional<TypeMirror> nonNull(Supplier<Class<?>> type) {
        return Optional.of(ProcessorUtil.extractResultType(type)).filter(this::isNonNullValue);
    }

    private boolean isNonNullValue(TypeMirror type) {
        switch (type.toString()) {
            case "nbbrd.service.ServiceDefinition.NoProcessing":
            case "java.lang.Void":
                return false;
            default:
                return true;
        }
    }
}
