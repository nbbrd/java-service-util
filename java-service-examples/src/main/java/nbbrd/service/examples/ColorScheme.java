package nbbrd.service.examples;

import nbbrd.service.Mutability;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@ServiceDefinition(
        backend = NetBeansLookup.class,
        cleaner = NetBeansLookup.class,
        mutability = Mutability.BASIC,
        singleton = true
)
public interface ColorScheme {

    List<Color> getColors();

    @ServiceProvider
    final class RGB implements ColorScheme {
        @Override
        public List<Color> getColors() {
            return Arrays.asList(Color.RED, Color.GREEN, Color.BLUE);
        }
    }

    static void main(String[] args) {
        ColorSchemeLoader.get().ifPresent(service -> System.out.println(service.getColors()));
        ColorSchemeLoader.get().ifPresent(service -> System.out.println(service.getColors()));
        ColorSchemeLoader.reload();
        ColorSchemeLoader.get().ifPresent(service -> System.out.println(service.getColors()));
    }
}
