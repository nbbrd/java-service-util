package definition;

import nbbrd.service.ServiceDefinition;

class TestFallbackNonAssignable {

    @ServiceDefinition(fallback = String.class)
    interface HelloService {
    }
}
