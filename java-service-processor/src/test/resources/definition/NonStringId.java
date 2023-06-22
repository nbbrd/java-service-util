package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface NonStringId {

    @ServiceId
    Object getName();
}
