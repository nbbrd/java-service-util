package definition;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

class TestPreprocessorNonInstantiable {

    @ServiceDefinition(quantifier = Quantifier.SINGLE, preprocessor = SomePreprocessor.class)
    interface SomeService {
    }

    static class SomePreprocessor implements UnaryOperator<Stream<SomeService>> {

        private SomePreprocessor() {
        }

        @Override
        public Stream<SomeService> apply(Stream<SomeService> t) {
            return t;
        }
    }
}
