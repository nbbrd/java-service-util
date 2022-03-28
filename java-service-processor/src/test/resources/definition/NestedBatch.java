package definition;

import nbbrd.service.ServiceDefinition;

import java.util.stream.Stream;

class NestedBatch {

    @ServiceDefinition(batch = true)
    interface HelloService {
    }

    //@ServiceProvider
    public static class ABC implements NestedBatchBatch.HelloService {
        @Override
        public Stream<HelloService> getProviders() {
            return Stream.empty();
        }
    }
}
