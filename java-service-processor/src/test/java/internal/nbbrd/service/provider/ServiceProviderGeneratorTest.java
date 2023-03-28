package internal.nbbrd.service.provider;

import org.junit.jupiter.api.Test;

import static internal.nbbrd.service.provider.ServiceProviderGenerator.merge;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ServiceProviderGeneratorTest {

    @Test
    public void testMerge() {
        assertThat(merge(asList("a", "b"), asList("c", "d")))
                .containsExactly("a", "b", "c", "d");

        assertThat(merge(asList("a", "b"), asList("a", "d")))
                .containsExactly("a", "b", "d");

        assertThat(merge(asList("a", "b"), asList("c", "a")))
                .containsExactly("a", "b", "c");

        assertThat(merge(asList("a", "b"), asList("c", "c")))
                .containsExactly("a", "b", "c");

        assertThat(merge(asList("b", "a"), asList("c", "c")))
                .containsExactly("a", "b", "c");
    }
}
