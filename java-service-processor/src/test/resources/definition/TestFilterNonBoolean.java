package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

@ServiceDefinition
interface TestFilterNonBoolean {

    @ServiceFilter
    int isAvailable();
}
