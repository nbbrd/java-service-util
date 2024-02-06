package definition;

import nbbrd.service.ServiceDefinition;

import java.util.Collections;
import java.util.function.Function;

class TestBackendNonAssignable {

    @ServiceDefinition(backend = HelloProc.class)
    interface HelloService {
    }

    public static class HelloProc implements Function<Class<Integer>, Iterable<String>> {

        @Override
        public Iterable<String> apply(Class<Integer> type) {
            return Collections.emptyList();
        }
    }
}
