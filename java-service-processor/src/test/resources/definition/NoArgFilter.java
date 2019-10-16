package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

@ServiceDefinition
interface NoArgFilter {

    @ServiceFilter
    boolean isAvailable(String arg);
}
