package internal.nbbrd.service;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import java.util.function.Consumer;
import java.util.function.Function;

public final class TypeNames {

    private TypeNames() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(Object.class);

    public static final ParameterizedTypeName WILDCARD_CLASS = typeOf(Class.class, WILDCARD);

    public static ParameterizedTypeName functionOf(TypeName input, TypeName result) {
        return typeOf(Function.class, input, result);
    }

    public static ParameterizedTypeName consumerOf(TypeName input) {
        return typeOf(Consumer.class, input);
    }

    public static ParameterizedTypeName iterableOf(TypeName element) {
        return typeOf(Iterable.class, element);
    }

    public static ParameterizedTypeName typeOf(Class<?> rawType, TypeName... typeArguments) {
        return ParameterizedTypeName.get(ClassName.get(rawType), typeArguments);
    }
}
