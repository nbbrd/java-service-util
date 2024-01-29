package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdUniqueness {

    @ServiceId
    String getName();

    @ServiceId
    String getDescription();
}
