package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdWithInvalidPattern {

    @ServiceId( pattern = "***" )
    String getName();
}
