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
package internal.nbbrd.service.provider;

import internal.nbbrd.service.ProcessorUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import nbbrd.service.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
final class AnnotationRegistry implements ProviderRegistry {

    @lombok.NonNull
    private final Set<? extends TypeElement> annotations;

    @lombok.NonNull
    private final RoundEnvironment roundEnv;

    public List<ProviderRef> readAll() {
        return annotations.stream()
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream)
                .map(TypeElement.class::cast)
                .flatMap(AnnotationRegistry::newRefs)
                .collect(Collectors.toList());
    }

    static Stream<ProviderRef> newRefs(TypeElement type) {
        return getAnnotations(type)
                .map(annotation -> getServiceType(annotation, type))
                .map(service -> new ProviderRef(service, type));
    }

    static Stream<ServiceProvider> getAnnotations(TypeElement type) {
        ServiceProvider.List list = type.getAnnotation(ServiceProvider.List.class);
        return list == null
                ? Stream.of(type.getAnnotation(ServiceProvider.class))
                : Stream.of(list.value());
    }

    static TypeElement getServiceType(ServiceProvider annotation, TypeElement type) {
        return (TypeElement) ((DeclaredType) getServiceTypeMirror(annotation, type)).asElement();
    }

    static TypeMirror getServiceTypeMirror(ServiceProvider annotation, TypeElement type) {
        TypeMirror serviceType = ProcessorUtil.extractResultType(annotation::value);
        return isNullValue(serviceType)
                ? inferServiceType(type).orElse(serviceType)
                : serviceType;
    }

    static boolean isNullValue(TypeMirror serviceType) {
        return serviceType.toString().equals(Void.class.getName());
    }

    static Optional<TypeMirror> inferServiceType(TypeElement type) {
        List<? extends TypeMirror> parents = type.getInterfaces();
        return parents.size() == 1 ? Optional.of(parents.get(0)) : Optional.empty();
    }
}
