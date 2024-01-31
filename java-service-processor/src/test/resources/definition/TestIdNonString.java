package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdNonString {

    @ServiceId
    Object getName();
}
