package definition;

import static nbbrd.service.Mutability.*;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

class TestBatchReloading {

    @ServiceDefinition(batchType = Batch.class, mutability = CONCURRENT)
    interface Mutable {
    }

    interface Batch {
        Stream<Mutable> getProviders();
    }
}
