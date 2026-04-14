package definition;

import nbbrd.service.ServiceDefinition;

import java.util.Collection;

@ServiceDefinition(batchType = TestBatchCollectionReturnType.SomeBatch.class)
interface TestBatchCollectionReturnType {

    interface SomeBatch {

        Collection<TestBatchCollectionReturnType> getProviders();
    }
}
