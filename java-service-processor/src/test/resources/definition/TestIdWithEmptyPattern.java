package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdWithEmptyPattern {

    @ServiceId( pattern = "" )
    String getName();
}
