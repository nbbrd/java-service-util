package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class MultipleDef {

    @ServiceDefinition(quantifier = MULTIPLE, mutability = NONE)
    interface Immutable {
    }

    @ServiceDefinition(quantifier = MULTIPLE, mutability = BASIC)
    interface Mutable {
    }

    @ServiceDefinition(quantifier = MULTIPLE, mutability = CONCURRENT)
    interface ThreadSafe {
    }

    @ServiceDefinition(quantifier = MULTIPLE, mutability = NONE, singleton = true)
    interface ImmutableSingleton {
    }

    @ServiceDefinition(quantifier = MULTIPLE, mutability = BASIC, singleton = true)
    interface MutableSingleton {
    }

    @ServiceDefinition(quantifier = MULTIPLE, mutability = CONCURRENT, singleton = true)
    interface ThreadSafeSingleton {
    }
}
