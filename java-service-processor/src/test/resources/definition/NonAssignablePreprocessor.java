package definition;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import nbbrd.service.ServiceDefinition;

class NonAssignablePreprocessor {

    @ServiceDefinition(preprocessor = HelloProc.class)
    interface HelloService {
    }

    public static class HelloProc implements UnaryOperator<Stream> {

        @Override
        public Stream apply(Stream t) {
            return t;
        }
    }
}
