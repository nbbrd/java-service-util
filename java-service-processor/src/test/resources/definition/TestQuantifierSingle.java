package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class TestQuantifierSingle {

    @ServiceDefinition(quantifier = SINGLE, mutability = NONE)
    interface Immutable {
    }

    @ServiceDefinition(quantifier = SINGLE, mutability = BASIC)
    interface Mutable {
    }

    @ServiceDefinition(quantifier = SINGLE, mutability = CONCURRENT)
    interface ThreadSafe {
    }
}
