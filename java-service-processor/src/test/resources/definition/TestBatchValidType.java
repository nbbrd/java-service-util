package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

@ServiceDefinition(batchType = TestBatchValidType.SomeBatch.class)
interface TestBatchValidType {

    interface SomeBatch {

        Stream<TestBatchValidType> getProviders();
    }
}
