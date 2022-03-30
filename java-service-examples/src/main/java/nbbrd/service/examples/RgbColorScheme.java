package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.util.Arrays;
import java.util.List;

@ServiceDefinition(
        quantifier = Quantifier.MULTIPLE
)
public interface RgbColorScheme {

    List<Integer> getColors();

    @ServiceProvider
    final class RedGreenBlue implements RgbColorScheme {
        @Override
        public List<Integer> getColors() {
            return Arrays.asList(-65536, -16711936, -16776961);
        }
    }

    public static void main(String[] args) {
        RgbColorSchemeLoader.load().forEach(colorScheme -> System.out.println(colorScheme.getColors()));
    }
}
