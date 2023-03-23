package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class BatchReloading {

    @ServiceDefinition(batch = true, mutability = CONCURRENT)
    interface Mutable {
    }

    @ServiceDefinition(batch = true, mutability = CONCURRENT, singleton = true)
    interface MutableSingleton {
    }
}
