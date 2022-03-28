package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceDefinition(
        batch = true,
        quantifier = Quantifier.MULTIPLE
)
public interface SwingColorScheme {

    List<Color> getColors();

    @ServiceProvider
    final class Black implements SwingColorScheme {

        @Override
        public List<Color> getColors() {
            return Collections.singletonList(Color.BLACK);
        }
    }

    //    @ServiceProvider(SwingColorSchemeBatch.class)
    final class Bridge implements SwingColorSchemeBatch {

        @Override
        public Stream<SwingColorScheme> getProviders() {
            return RgbColorSchemeLoader.load().stream().map(this::convert);
        }

        private SwingColorScheme convert(RgbColorScheme colorScheme) {
            return () -> colorScheme.getColors().stream().map(Color::new).collect(Collectors.toList());
        }
    }

    public static void main(String[] args) {
        SwingColorSchemeLoader.load().forEach(colorScheme -> System.out.println(colorScheme.getColors()));
    }
}
