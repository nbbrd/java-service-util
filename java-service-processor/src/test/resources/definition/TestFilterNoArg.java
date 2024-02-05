package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

@ServiceDefinition
interface TestFilterNoArg {

    @ServiceFilter
    boolean isAvailable(String arg);
}
