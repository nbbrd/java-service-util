package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdEmptyPattern {

    @ServiceId( pattern = "" )
    String getName();
}
