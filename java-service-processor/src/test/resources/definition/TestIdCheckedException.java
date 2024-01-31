package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

import java.io.IOException;

@ServiceDefinition
interface TestIdCheckedException {

    @ServiceId
    String getName() throws IOException;
}
