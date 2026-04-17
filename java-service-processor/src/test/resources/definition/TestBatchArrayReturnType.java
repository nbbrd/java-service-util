package definition;

import nbbrd.service.ServiceDefinition;

@ServiceDefinition(batchType = TestBatchArrayReturnType.SomeBatch.class)
interface TestBatchArrayReturnType {

    interface SomeBatch {

        TestBatchArrayReturnType[] getProviders();
    }
}

