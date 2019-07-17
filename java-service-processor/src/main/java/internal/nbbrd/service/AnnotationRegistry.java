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
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
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
                .map(AnnotationRegistry::newRef)
                .collect(Collectors.toList());
    }

    static ProviderRef newRef(TypeElement type) {
        TypeMirror serviceType = extractResultType(type.getAnnotation(ServiceProvider.class)::value);
        Name serviceName = ((TypeElement) ((DeclaredType) serviceType).asElement()).getQualifiedName();
        Name providerName = type.getQualifiedName();
        return new ProviderRef(serviceName, providerName);
    }

    // see http://hauchee.blogspot.be/2015/12/compile-time-annotation-processing-getting-class-value.html
    static TypeMirror extractResultType(Supplier<Class<?>> type) {
        try {
            type.get();
            throw new RuntimeException("Expecting exeption to be raised");
        } catch (MirroredTypeException ex) {
            return ex.getTypeMirror();
        }
    }
}
