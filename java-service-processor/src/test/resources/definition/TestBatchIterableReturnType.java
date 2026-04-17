package definition;

import nbbrd.service.ServiceDefinition;

@ServiceDefinition(batchType = TestBatchIterableReturnType.SomeBatch.class)
interface TestBatchIterableReturnType {

    interface SomeBatch {

        Iterable<TestBatchIterableReturnType> getProviders();
    }
}

