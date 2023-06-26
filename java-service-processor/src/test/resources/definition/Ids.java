package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

class Ids {

    @ServiceDefinition
    interface SimpleId {

        @ServiceId
        String getName();
    }
}
