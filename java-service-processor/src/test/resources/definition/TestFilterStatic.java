package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

@ServiceDefinition
interface TestFilterStatic {

    @ServiceFilter
    static boolean isAvailable() {
        return false;
    }
}
