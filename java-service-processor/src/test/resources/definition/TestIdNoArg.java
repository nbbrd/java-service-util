package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdNoArg {

    @ServiceId
    String getName(String arg);
}
