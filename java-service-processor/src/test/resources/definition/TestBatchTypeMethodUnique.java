package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

@ServiceDefinition(batchType = TestBatchTypeMethodUnique.SomeBatch.class)
interface TestBatchTypeMethodUnique {

    interface SomeBatch {

        Stream<String> getProviders();
    }
}
