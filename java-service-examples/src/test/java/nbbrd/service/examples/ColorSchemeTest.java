package nbbrd.service.examples;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class ColorSchemeTest {

    @Test
    public void test() {
        Optional<ColorScheme> original = ColorSchemeLoader.get();
        ColorScheme modified = Collections::emptyList;

        assertThatCode(() -> ColorSchemeLoader.set(Optional.of(modified))).doesNotThrowAnyException();

        assertThat(ColorSchemeLoader.get()).hasValue(modified);

        assertThatCode(() -> ColorSchemeLoader.set(Optional.empty())).doesNotThrowAnyException();

        assertThat(ColorSchemeLoader.get()).isEmpty();

        assertThatCode(() -> ColorSchemeLoader.reset()).doesNotThrowAnyException();

        assertThat(ColorSchemeLoader.get()).isEqualTo(original);

        assertThatCode(() -> ColorSchemeLoader.reload()).doesNotThrowAnyException();
    }
}
