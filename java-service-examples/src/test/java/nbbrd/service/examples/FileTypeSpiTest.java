package nbbrd.service.examples;

import internal.nbbrd.service.examples.FileTypeSpiLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class FileTypeSpiTest {

    @Test
    public void test() {
        FileTypeSpiLoader x = new FileTypeSpiLoader();

        assertThat(x.get())
                .containsExactlyElementsOf(x.get())
                .hasExactlyElementsOfTypes(FileType.ByMagicNumberProbe.class, FileType.ByExtensionProbe.class)
                .extracting(FileTypeSpiTest::getTypeName)
                .containsExactlyElementsOf(toTypeNames(FileTypeSpiLoader.load()))
                .containsExactlyInAnyOrderElementsOf(toTypeNames(ServiceLoader.load(FileType.FileTypeSpi.class)));

        assertThatCode(x::reload)
                .doesNotThrowAnyException();
    }

    private static List<String> toTypeNames(Iterable<?> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(FileTypeSpiTest::getTypeName)
                .collect(toList());
    }

    private static String getTypeName(Object fileTypeSpi) {
        return fileTypeSpi.getClass().getName();
    }
}
