package definition;

import nbbrd.service.ServiceDefinition;

class TestWrapperNonAssignable {

    @ServiceDefinition(wrapper = String.class)
    interface HelloService {
    }
}
