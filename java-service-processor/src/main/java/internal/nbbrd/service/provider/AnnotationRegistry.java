package internal.nbbrd.service.provider;

import internal.nbbrd.service.ProcessorUtil;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import nbbrd.service.ServiceProvider;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .flatMap(AnnotationRegistry::newRefs)
                .collect(Collectors.toList());
    }

    static Stream<ProviderRef> newRefs(Element element) {
        ElementKind kind = element.getKind();

        if (kind == ElementKind.CLASS || kind == ElementKind.ENUM || kind == ElementKind.INTERFACE) {
            // TYPE annotation: no delegate needed
            TypeElement type = (TypeElement) element;
            return getAnnotations(element)
                    .map(annotation -> getServiceType(annotation, type))
                    .map(service -> new ProviderRef(service, type, Optional.empty(), Optional.empty()));
        } else if (kind == ElementKind.FIELD || kind == ElementKind.METHOD) {
            // FIELD or METHOD annotation: delegate wrapper needed
            if (!element.getModifiers().contains(Modifier.STATIC)) {
                return Stream.empty(); // Skip non-static; checker will report error
            }
            TypeElement enclosingClass = (TypeElement) element.getEnclosingElement();
            return getAnnotations(element)
                    .map(annotation -> getServiceTypeForDelegateSource(annotation, element))
                    .map(service -> new ProviderRef(service, enclosingClass, Optional.of(element), Optional.empty()));
        } else {
            return Stream.empty();
        }
    }

    static Stream<ServiceProvider> getAnnotations(Element element) {
        ServiceProvider.List list = element.getAnnotation(ServiceProvider.List.class);
        return list == null
                ? Stream.of(element.getAnnotation(ServiceProvider.class)).filter(a -> a != null)
                : Stream.of(list.value());
    }

    static TypeElement getServiceTypeForDelegateSource(ServiceProvider annotation, Element delegateSource) {
        TypeMirror serviceType = ProcessorUtil.extractResultType(annotation::value);
        if (isNullValue(serviceType)) {
            // Infer from the field/method type
            if (delegateSource.getKind() == ElementKind.FIELD) {
                serviceType = ((VariableElement) delegateSource).asType();
            } else if (delegateSource.getKind() == ElementKind.METHOD) {
                serviceType = ((ExecutableElement) delegateSource).getReturnType();
            }
        }
        return (TypeElement) ((DeclaredType) serviceType).asElement();
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
