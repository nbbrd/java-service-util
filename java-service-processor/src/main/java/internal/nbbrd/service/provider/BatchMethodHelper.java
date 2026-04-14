package internal.nbbrd.service.provider;

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
import java.util.stream.Stream;

/**
 * Helper to resolve batch method return kinds.
 */
@lombok.experimental.UtilityClass
class BatchMethodHelper {

    enum ReturnKind {
        STREAM, COLLECTION, ITERABLE, ITERATOR, ARRAY
    }

    static @Nullable ReturnKind resolve(TypeMirror returnType, TypeElement serviceType, Types types, Elements elements) {
        DeclaredType streamType = types.getDeclaredType(elements.getTypeElement(Stream.class.getCanonicalName()), serviceType.asType());
        if (types.isAssignable(returnType, streamType)) return ReturnKind.STREAM;
        DeclaredType collectionType = types.getDeclaredType(elements.getTypeElement(Collection.class.getCanonicalName()), serviceType.asType());
        if (types.isAssignable(returnType, collectionType)) return ReturnKind.COLLECTION;
        DeclaredType iterableType = types.getDeclaredType(elements.getTypeElement(Iterable.class.getCanonicalName()), serviceType.asType());
        if (types.isAssignable(returnType, iterableType)) return ReturnKind.ITERABLE;
        DeclaredType iteratorType = types.getDeclaredType(elements.getTypeElement(Iterator.class.getCanonicalName()), serviceType.asType());
        if (types.isAssignable(returnType, iteratorType)) return ReturnKind.ITERATOR;
        if (returnType.getKind() == TypeKind.ARRAY) {
            TypeMirror componentType = ((ArrayType) returnType).getComponentType();
            if (types.isAssignable(componentType, types.erasure(serviceType.asType()))) return ReturnKind.ARRAY;
        }
        return null;
    }
}

