package provider;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.util.stream.Stream;

public class EnumBatchProvider {

    @ServiceDefinition(quantifier = Quantifier.MULTIPLE, batchType = ColorBatch.class)
    public interface Color {
        String getName();
    }

    public interface ColorBatch {
        Stream<Color> getColors();
    }

    @ServiceProvider
    public enum PrimaryColor implements Color {
        RED, GREEN, BLUE;

        @Override
        public String getName() {
            return name();
        }
    }
}

