package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

@ServiceDefinition(batchType = TestBatchPropertyType.SomeBatch.class, batch = true)
interface TestBatchPropertyType {

    interface SomeBatch {

        Stream<TestBatchPropertyType> getProviders();
    }
}
