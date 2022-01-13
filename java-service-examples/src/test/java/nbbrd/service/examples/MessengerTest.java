package nbbrd.service.examples;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class MessengerTest {

    @Test
    public void test() {
        MessengerLoader loader = new MessengerLoader();

        Optional<Messenger> original = loader.get();
        Messenger modified = System.out::println;

        assertThatCode(() -> loader.set(Optional.of(modified))).doesNotThrowAnyException();

        assertThat(loader.get()).hasValue(modified);

        assertThatCode(() -> loader.set(Optional.empty())).doesNotThrowAnyException();

        assertThat(loader.get()).isEmpty();

        assertThatCode(() -> loader.reset()).doesNotThrowAnyException();

        assertThat(loader.get()).isEqualTo(original);

        assertThatCode(() -> loader.reload()).doesNotThrowAnyException();
    }
}
