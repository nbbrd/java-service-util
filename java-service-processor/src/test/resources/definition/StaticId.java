package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface StaticId {

    @ServiceId
    static String getName() {
        return "";
    }
}
