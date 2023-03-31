package internal.nbbrd.service.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class ClassPathRefTest {

    @Test
    public void testToString() {
        assertThat(new ClassPathRef("", null))
                .hasToString("");

        assertThat(new ClassPathRef("", ""))
                .hasToString(" #");

        assertThat(new ClassPathRef("", " comment"))
                .hasToString(" # comment");

        assertThat(new ClassPathRef("provider", null))
                .hasToString("provider");

        assertThat(new ClassPathRef("provider", ""))
                .hasToString("provider #");

        assertThat(new ClassPathRef("provider", " comment"))
                .hasToString("provider # comment");
    }

    @Test
    public void testParse() {
        assertThat(ClassPathRef.parse(""))
                .isEqualTo(new ClassPathRef("", null));

        assertThat(ClassPathRef.parse("#"))
                .isEqualTo(new ClassPathRef("", ""));

        assertThat(ClassPathRef.parse(" #"))
                .isEqualTo(new ClassPathRef("", ""));

        assertThat(ClassPathRef.parse(" # comment"))
                .isEqualTo(new ClassPathRef("", " comment"));

        assertThat(ClassPathRef.parse("provider"))
                .isEqualTo(new ClassPathRef("provider", null));

        assertThat(ClassPathRef.parse("provider#"))
                .isEqualTo(new ClassPathRef("provider", ""));

        assertThat(ClassPathRef.parse("provider #"))
                .isEqualTo(new ClassPathRef("provider", ""));

        assertThat(ClassPathRef.parse("provider # comment"))
                .isEqualTo(new ClassPathRef("provider", " comment"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> ClassPathRef.parse("pro vider"))
                .withMessageContaining("Illegal configuration-file syntax");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> ClassPathRef.parse("pro+vider"))
                .withMessageContaining("Illegal provider-class name");
    }
}
