package nbbrd.service.examples;

import internal.FileTypeSpiLoader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

public class FileTypeSpiTest {

    @Test
    public void test() {
        FileTypeSpiLoader x = new FileTypeSpiLoader();

        assertThat(x.get())
                .containsExactlyElementsOf(x.get())
                .hasSize(2)
                .satisfies(o -> assertThat(o).isInstanceOf(FileTypeSpi.ByMagicNumberProbe.class), atIndex(0))
                .satisfies(o -> assertThat(o).isInstanceOf(FileTypeSpi.ByExtensionProbe.class), atIndex(1));
    }
}
