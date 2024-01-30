package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition
interface TestIdStaticMethod {

    @ServiceId
    static String getName() {
        return "";
    }
}
