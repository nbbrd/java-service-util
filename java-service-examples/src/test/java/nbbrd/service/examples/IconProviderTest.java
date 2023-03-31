package nbbrd.service.examples;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

public class IconProviderTest {

    @Test
    public void test() {
        assertThat(ServiceLoader.load(IconProvider.class))
                .hasSize(1);

        IconProviderLoader x = new IconProviderLoader();

        assertThat(x.get().get())
                .isEqualTo(x.get().get())
                .isInstanceOf(IconProvider.MetalIconProvider.class);
    }
}
