package definition;

import nbbrd.service.ServiceDefinition;

import java.util.Iterator;

@ServiceDefinition(batchType = TestBatchIteratorReturnType.SomeBatch.class)
interface TestBatchIteratorReturnType {

    interface SomeBatch {

        Iterator<TestBatchIteratorReturnType> getProviders();
    }
}

