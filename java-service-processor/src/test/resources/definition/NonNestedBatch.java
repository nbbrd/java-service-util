package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.util.stream.Stream;

@ServiceDefinition(batch = true)
class NonNestedBatch {

//    @ServiceProvider
    public static class ABC implements NonNestedBatchBatch {
        @Override
        public Stream<NonNestedBatch> getProviders() {
            return Stream.empty();
        }
    }
}
