package internal.nbbrd.service;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Blocks {

    private Blocks() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static CodeBlock casting(TypeName type, CodeBlock block) {
        return CodeBlock.of("(($T)$L)", type, block);
    }

    public static CodeBlock iterableToStream(FieldSpec iterable) {
        return CodeBlock.of("$T.stream($N.spliterator(), false)", StreamSupport.class, iterable);
    }

    public static CodeBlock concatStreams(CodeBlock first, CodeBlock second) {
        return CodeBlock.of("$T.concat($L, $L)", Stream.class, first, second);
    }

    public static CodeBlock flatMapStream(CodeBlock stream, CodeBlock mapper) {
        return CodeBlock.of("$L.flatMap($L)", stream, mapper);
    }

    public static CodeBlock filterStream(CodeBlock stream, CodeBlock predicate) {
        return CodeBlock.of("$L.filter($L)", stream, predicate);
    }
}
