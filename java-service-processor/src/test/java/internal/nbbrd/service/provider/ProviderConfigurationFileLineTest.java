package internal.nbbrd.service.provider;

import org.assertj.core.util.URLs;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static internal.nbbrd.service.provider.ProviderConfigurationFileLine.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

public class ProviderConfigurationFileLineTest {

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testFactories() {
        assertThat(EMPTY)
                .extracting(ProviderConfigurationFileLine::getProviderBinaryName, ProviderConfigurationFileLine::getComment)
                .containsExactly(null, null);

        assertThatNullPointerException()
                .isThrownBy(() -> ofProviderBinaryName(null));

        assertThat(ofProviderBinaryName("abc"))
                .extracting(ProviderConfigurationFileLine::getProviderBinaryName, ProviderConfigurationFileLine::getComment)
                .containsExactly("abc", null);

        assertThatNullPointerException()
                .isThrownBy(() -> ofComment(null));

        assertThat(ofComment("def"))
                .extracting(ProviderConfigurationFileLine::getProviderBinaryName, ProviderConfigurationFileLine::getComment)
                .containsExactly(null, "def");


        assertThatNullPointerException()
                .isThrownBy(() -> ofProviderBinaryNameAndComment(null, null));

        assertThat(ofProviderBinaryNameAndComment("abc", "def"))
                .extracting(ProviderConfigurationFileLine::getProviderBinaryName, ProviderConfigurationFileLine::getComment)
                .containsExactly("abc", "def");
    }

    @Test
    public void testToString() {
        assertThat(EMPTY)
                .hasToString("");

        assertThat(ofComment(""))
                .hasToString("#");

        assertThat(ofComment(" comment"))
                .hasToString("# comment");

        assertThat(ofProviderBinaryName("provider"))
                .hasToString("provider");

        assertThat(ofProviderBinaryNameAndComment("provider", ""))
                .hasToString("provider #");

        assertThat(ofProviderBinaryNameAndComment("provider", " comment"))
                .hasToString("provider # comment");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testParse() {
        assertThatNullPointerException()
                .isThrownBy(() -> parse(null));

        assertThat(parse(""))
                .isEqualTo(EMPTY);

        assertThat(parse(" "))
                .isEqualTo(EMPTY);

        assertThat(parse("#"))
                .isEqualTo(ofComment(""));

        assertThat(parse(" #"))
                .isEqualTo(ofComment(""));

        assertThat(parse(" # comment"))
                .isEqualTo(ofComment(" comment"));

        assertThat(parse("provider"))
                .isEqualTo(ofProviderBinaryName("provider"));

        assertThat(parse("provider#"))
                .isEqualTo(ofProviderBinaryNameAndComment("provider", ""));

        assertThat(parse("provider #"))
                .isEqualTo(ofProviderBinaryNameAndComment("provider", ""));

        assertThat(parse("provider # comment"))
                .isEqualTo(ofProviderBinaryNameAndComment("provider", " comment"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> parse("pro vider"))
                .withMessageContaining("Illegal configuration-file syntax");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> parse("pro+vider"))
                .withMessageContaining("Illegal provider-class name");
    }

    @Test
    public void testNetBeans() {
        URL resource = Objects.requireNonNull(ProviderConfigurationFileLine.class.getResource("/provider/NetBeansClassPathRef"));
        List<ProviderConfigurationFileLine> list = asList(
                ofProviderBinaryName("nbbrd.service.examples.IconProvider$NullProvider"),
                ofComment("position=123"),
                ofComment("-nbbrd.service.examples.MetalIconProvider")
        );

        assertThat(URLs.linesOf(resource, StandardCharsets.UTF_8))
                .map(ProviderConfigurationFileLine::parse)
                .containsExactlyElementsOf(list);

        assertThat(list)
                .map(ProviderConfigurationFileLine::toString)
                .containsExactlyElementsOf(URLs.linesOf(resource, StandardCharsets.UTF_8));
    }
}
