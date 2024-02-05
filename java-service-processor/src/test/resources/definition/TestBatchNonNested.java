package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.util.stream.Stream;

@ServiceDefinition(batch = true)
class TestBatchNonNested {

    @ServiceProvider
    public static class ABC implements TestBatchNonNestedBatch {
        @Override
        public Stream<TestBatchNonNested> getProviders() {
            return Stream.empty();
        }
    }
}
