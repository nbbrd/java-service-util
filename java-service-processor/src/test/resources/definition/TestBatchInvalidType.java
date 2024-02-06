package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

@ServiceDefinition(batchType = TestBatchInvalidType.SomeBatch.class)
interface TestBatchInvalidType {

    class SomeBatch {

        Stream<TestBatchInvalidType> getProviders();
    }
}
