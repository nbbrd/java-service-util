package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

class TestBatchReloading {

    @ServiceDefinition(batchType = Batch.class)
    interface Mutable {
    }

    interface Batch {
        Stream<Mutable> getProviders();
    }
}
