package internal.nbbrd.service.provider;

import org.junit.jupiter.api.Test;

import static internal.nbbrd.service.provider.ProviderConfigurationFileLine.ofProviderBinaryName;
import static internal.nbbrd.service.provider.ServiceProviderGenerator.concat;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ServiceProviderGeneratorTest {

    @Test
    public void testConcat() {
        assertThat(concat(asList(a, b), asList(c, d)))
                .containsExactly(a, b, c, d);

        assertThat(concat(asList(a, b), asList(a, d)))
                .containsExactly(a, b, a, d);

        assertThat(concat(asList(a, b), asList(c, a)))
                .containsExactly(a, b, c, a);

        assertThat(concat(asList(a, b), asList(c, c)))
                .containsExactly(a, b, c, c);

        assertThat(concat(asList(b, a), asList(c, c)))
                .containsExactly(b, a, c, c);
    }

    private final ProviderConfigurationFileLine a = ofProviderBinaryName("a");
    private final ProviderConfigurationFileLine b = ofProviderBinaryName("b");
    private final ProviderConfigurationFileLine c = ofProviderBinaryName("c");
    private final ProviderConfigurationFileLine d = ofProviderBinaryName("d");
}
