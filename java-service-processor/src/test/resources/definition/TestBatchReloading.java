package definition;

import static nbbrd.service.Mutability.*;

import nbbrd.service.ServiceDefinition;

class TestBatchReloading {

    @ServiceDefinition(batch = true, mutability = CONCURRENT)
    interface Mutable {
    }

    @ServiceDefinition(batch = true, mutability = CONCURRENT, singleton = true)
    interface MutableSingleton {
    }
}
