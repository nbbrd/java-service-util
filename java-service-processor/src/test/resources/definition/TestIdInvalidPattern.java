package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdInvalidPattern {

    @ServiceId( pattern = "***" )
    String getName();
}
