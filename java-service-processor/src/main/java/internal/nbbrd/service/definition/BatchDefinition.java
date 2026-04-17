package internal.nbbrd.service.definition;

import org.jspecify.annotations.Nullable;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Describes the batch type and its single batch method.
 *
 * @author Philippe Charles
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@lombok.Value
class BatchDefinition {

    @lombok.NonNull
    TypeMirror type;

    @lombok.NonNull
    Optional<String> methodName;

    @lombok.NonNull
    Optional<MethodReturnKind> methodReturnKind;

    public enum MethodReturnKind {
        /** Method returns {@code Stream<Service>}. */
        STREAM,
        /** Method returns {@code Collection<Service>}. */
        COLLECTION,
        /** Method returns {@code Iterable<Service>}. */
        ITERABLE,
        /** Method returns {@code Iterator<Service>}. */
        ITERATOR,
        /** Method returns {@code Service[]}. */
        ARRAY;

        static @Nullable MethodReturnKind resolve(TypeMirror returnType, TypeElement serviceType, Types types, Elements elements) {
            DeclaredType streamType = types.getDeclaredType(elements.getTypeElement(Stream.class.getCanonicalName()), serviceType.asType());
            if (types.isAssignable(returnType, streamType)) return STREAM;
            DeclaredType collectionType = types.getDeclaredType(elements.getTypeElement(Collection.class.getCanonicalName()), serviceType.asType());
            if (types.isAssignable(returnType, collectionType)) return COLLECTION;
            DeclaredType iterableType = types.getDeclaredType(elements.getTypeElement(Iterable.class.getCanonicalName()), serviceType.asType());
            if (types.isAssignable(returnType, iterableType)) return ITERABLE;
            DeclaredType iteratorType = types.getDeclaredType(elements.getTypeElement(Iterator.class.getCanonicalName()), serviceType.asType());
            if (types.isAssignable(returnType, iteratorType)) return ITERATOR;
            if (returnType.getKind() == TypeKind.ARRAY) {
                TypeMirror componentType = ((ArrayType) returnType).getComponentType();
                if (types.isAssignable(componentType, types.erasure(serviceType.asType()))) return ARRAY;
            }
            return null;
        }
    }
}
