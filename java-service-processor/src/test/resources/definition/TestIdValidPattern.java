package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdValidPattern {

    @ServiceId( pattern = ServiceId.SCREAMING_SNAKE_CASE )
    String getName();
}
