package internal.nbbrd.service.provider;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ServiceProviderGeneratorTest {

    @Test
    public void testMerge() {
        assertThat(ServiceProviderGenerator.merge(asList("a", "b"), asList("c", "d")))
                .containsExactly("a", "b", "c", "d");

        assertThat(ServiceProviderGenerator.merge(asList("a", "b"), asList("a", "d")))
                .containsExactly("a", "b", "d");

        assertThat(ServiceProviderGenerator.merge(asList("a", "b"), asList("c", "a")))
                .containsExactly("a", "b", "c");

        assertThat(ServiceProviderGenerator.merge(asList("a", "b"), asList("c", "c")))
                .containsExactly("a", "b", "c");
    }
}
