package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class TestQuantifierOptional {

    @ServiceDefinition(quantifier = OPTIONAL, mutability = NONE)
    interface Immutable {
    }

    @ServiceDefinition(quantifier = OPTIONAL, mutability = BASIC)
    interface Mutable {
    }

    @ServiceDefinition(quantifier = OPTIONAL, mutability = CONCURRENT)
    interface ThreadSafe {
    }
}
