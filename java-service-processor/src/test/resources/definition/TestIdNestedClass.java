package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

class TestIdNestedClass {

    @ServiceDefinition
    interface SimpleId {

        @ServiceId
        String getName();
    }
}
