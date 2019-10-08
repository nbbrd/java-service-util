package definition;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;

class UnknownFactory {

    @ServiceDefinition(quantifier = Quantifier.SINGLE, fallback = UnknownFallback.class)
    interface UnknownService {
    }

    static class UnknownFallback implements UnknownService {

        private UnknownFallback() {
        }
    }
}
