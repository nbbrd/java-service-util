package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class SingleDef {

    @ServiceDefinition(quantifier = SINGLE, mutability = NONE)
    interface Immutable {
    }

    @ServiceDefinition(quantifier = SINGLE, mutability = BASIC)
    interface Mutable {
    }

    @ServiceDefinition(quantifier = SINGLE, mutability = CONCURRENT)
    interface ThreadSafe {
    }

    @ServiceDefinition(quantifier = SINGLE, mutability = NONE, singleton = true)
    interface ImmutableSingleton {
    }

    @ServiceDefinition(quantifier = SINGLE, mutability = BASIC, singleton = true)
    interface MutableSingleton {
    }

    @ServiceDefinition(quantifier = SINGLE, mutability = CONCURRENT, singleton = true)
    interface ThreadSafeSingleton {
    }
}
