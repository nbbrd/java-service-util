package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

@ServiceDefinition
interface StaticFilter {

    @ServiceFilter
    static boolean isAvailable() {
        return false;
    }
}
