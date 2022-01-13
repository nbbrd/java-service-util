package nbbrd.service.examples;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggerFinderTest {

    @Test
    public void test() {
        LoggerFinderLoader x = new LoggerFinderLoader();

        assertThat(x.get())
                .isEqualTo(x.get())
                .isInstanceOf(LoggerFinder.FallbackLogger.class);
    }
}
