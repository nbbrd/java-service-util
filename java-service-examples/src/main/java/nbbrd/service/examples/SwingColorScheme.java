package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceDefinition(batch = true, quantifier = Quantifier.MULTIPLE)
public interface SwingColorScheme {

    List<Color> getColors();

    static void main(String[] args) {
        SwingColorSchemeLoader.load()
                .stream()
                .map(SwingColorScheme::getColors)
                .forEach(System.out::println);
    }

    @ServiceProvider(SwingColorSchemeBatch.class)
    final class RgbBridge implements SwingColorSchemeBatch {

        @Override
        public Stream<SwingColorScheme> getProviders() {
            return RgbColorSchemeLoader.load()
                    .stream()
                    .map(RgbAdapter::new);
        }
    }

    final class RgbAdapter implements SwingColorScheme {

        private final RgbColorScheme rgb;

        public RgbAdapter(RgbColorScheme rgb) {
            this.rgb = rgb;
        }

        @Override
        public List<Color> getColors() {
            return rgb.getColors()
                    .stream()
                    .map(Color::new)
                    .collect(Collectors.toList());
        }
    }
}
