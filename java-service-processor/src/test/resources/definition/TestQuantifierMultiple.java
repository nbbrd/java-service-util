package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class TestQuantifierMultiple {

    @ServiceDefinition(quantifier = MULTIPLE, mutability = NONE)
    interface Immutable {
    }

    @ServiceDefinition(quantifier = MULTIPLE, mutability = BASIC)
    interface Mutable {
    }

    @ServiceDefinition(quantifier = MULTIPLE, mutability = CONCURRENT)
    interface ThreadSafe {
    }
}
