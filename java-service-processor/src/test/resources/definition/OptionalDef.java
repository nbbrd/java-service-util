package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class OptionalDef {

    @ServiceDefinition(quantifier = OPTIONAL, mutability = NONE)
    interface Immutable {
    }

    @ServiceDefinition(quantifier = OPTIONAL, mutability = BASIC)
    interface Mutable {
    }

    @ServiceDefinition(quantifier = OPTIONAL, mutability = CONCURRENT)
    interface ThreadSafe {
    }

    @ServiceDefinition(quantifier = OPTIONAL, mutability = NONE, singleton = true)
    interface ImmutableSingleton {
    }

    @ServiceDefinition(quantifier = OPTIONAL, mutability = BASIC, singleton = true)
    interface MutableSingleton {
    }

    @ServiceDefinition(quantifier = OPTIONAL, mutability = CONCURRENT, singleton = true)
    interface ThreadSafeSingleton {
    }
}
