package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceDefinition(quantifier = Quantifier.MULTIPLE, batchType = SwingColorScheme.Batch.class)
public interface SwingColorScheme {

    List<Color> getColors();

    static void main(String[] args) {
        // 💡 Invisible use of RgbColorScheme
        SwingColorSchemeLoader.load()
                .stream()
                .map(SwingColorScheme::getColors)
                .forEach(System.out::println);
    }

    interface Batch {
        Stream<SwingColorScheme> getProviders();
    }

    // 💡 Bridge between SwingColorScheme and RgbColorScheme
    @ServiceProvider(Batch.class)
    final class RgbBridge implements Batch {

        @Override
        public Stream<SwingColorScheme> getProviders() {
            return RgbColorSchemeLoader.load()
                    .stream()
                    .map(RgbAdapter::new);
        }
    }

    // 💡 Regular provider
    @ServiceProvider(SwingColorScheme.class)
    final class Cyan implements SwingColorScheme {

        @Override
        public List<Color> getColors() {
            return Collections.singletonList(Color.CYAN);
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
