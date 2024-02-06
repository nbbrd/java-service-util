package definition;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

class TestFallbackNonInstantiable {

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = SomeFallback.class)
    interface SomeService {
    }

    static class SomeFallback implements SomeService {

        private SomeFallback() {
        }
    }
}
