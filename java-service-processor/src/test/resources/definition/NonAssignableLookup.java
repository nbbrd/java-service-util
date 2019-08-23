package definition;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import nbbrd.service.ServiceDefinition;

class NonAssignableLookup {

    @ServiceDefinition(lookup = HelloLookup.class)
    interface HelloService {
    }

    public static class HelloLookup implements UnaryOperator<Stream> {

        @Override
        public Stream apply(Stream t) {
            return t;
        }
    }
}
