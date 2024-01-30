package internal.nbbrd.service;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import static internal.nbbrd.service.Blocks.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BlocksTest {

    @Test
    public void testCasting() {
        assertThat(casting(TypeName.get(String.class), CodeBlock.of("obj")))
                .hasToString("((java.lang.String)obj)");
    }

    @Test
    public void testIterableToStream() {
        assertThat(iterableToStream(FieldSpec.builder(ParameterizedTypeName.get(Iterable.class, String.class), "hello").build()))
                .hasToString("java.util.stream.StreamSupport.stream(hello.spliterator(), false)");
    }

    @Test
    public void testConcatStreams() {
        assertThat(concatStreams(CodeBlock.of("first"), CodeBlock.of("second")))
                .hasToString("java.util.stream.Stream.concat(first, second)");
    }

    @Test
    public void testFlatMapStream() {
        assertThat(flatMapStream(CodeBlock.of("stream"), CodeBlock.of("o -> o.getStream()")))
                .hasToString("stream.flatMap(o -> o.getStream())");
    }

    @Test
    public void testFilterStream() {
        assertThat(filterStream(CodeBlock.of("stream"), CodeBlock.of("o -> o.isValid()")))
                .hasToString("stream.filter(o -> o.isValid())");
    }
}
