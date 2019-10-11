package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

@ServiceDefinition
interface NonBooleanFilter {

    @ServiceFilter
    int isAvailable();
}
