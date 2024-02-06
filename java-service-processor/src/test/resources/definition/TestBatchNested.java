package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceProvider;

import java.util.stream.Stream;

class TestBatchNested {

    @ServiceDefinition(batch = true)
    interface HelloService {
    }

    @ServiceProvider
    public static class ABC implements TestBatchNestedBatch.HelloService {
        @Override
        public Stream<HelloService> getProviders() {
            return Stream.empty();
        }
    }
}
