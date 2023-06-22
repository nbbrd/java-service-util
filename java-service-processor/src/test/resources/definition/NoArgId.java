package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface NoArgId {

    @ServiceId
    String getName(String arg);
}
