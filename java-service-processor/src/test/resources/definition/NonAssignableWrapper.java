package definition;

import nbbrd.service.ServiceDefinition;

class NonAssignableWrapper {

    @ServiceDefinition(wrapper = String.class)
    interface HelloService {
    }
}
