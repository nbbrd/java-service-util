package nbbrd.service.examples;

import internal.FileTypeSpiLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

public class FileTypeSpiTest {

    @Test
    public void test() {
        assertThat(FileTypeSpiLoader.get())
                .containsExactlyElementsOf(FileTypeSpiLoader.get())
                .hasSize(2)
                .satisfies(o -> assertThat(o).isInstanceOf(FileType.ByMagicNumberProbe.class), atIndex(0))
                .satisfies(o -> assertThat(o).isInstanceOf(FileType.ByExtensionProbe.class), atIndex(1));
    }
}
