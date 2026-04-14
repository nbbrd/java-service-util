package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

@ServiceDefinition(batchType = TestBatchTypeMethodNonUnique.SomeBatch.class)
interface TestBatchTypeMethodNonUnique {

    interface SomeBatch {

        Stream<TestBatchTypeMethodNonUnique> getProviders();

        Stream<TestBatchTypeMethodNonUnique> getAll();
    }
}

