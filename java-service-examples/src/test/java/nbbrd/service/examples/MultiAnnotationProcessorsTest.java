package nbbrd.service.examples;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiAnnotationProcessorsTest {

    @Test
    public void test() {
        assertThat(ServiceLoader.load(MultiAnnotationProcessors.class))
                .hasSize(1);
    }
}
