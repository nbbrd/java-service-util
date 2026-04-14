package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

@ServiceDefinition(batchType = TestBatchCustomMethodName.SomeBatch.class)
interface TestBatchCustomMethodName {

    interface SomeBatch {

        Stream<TestBatchCustomMethodName> getAll();
    }
}

