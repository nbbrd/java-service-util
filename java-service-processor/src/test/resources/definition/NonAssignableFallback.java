package definition;

import nbbrd.service.ServiceDefinition;

class NonAssignableFallback {

    @ServiceDefinition(fallback = String.class)
    interface HelloService {
    }
}
