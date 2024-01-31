package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

import java.io.IOException;

@ServiceDefinition
interface TestIdUncheckedException {

    @ServiceId
    String getName() throws IllegalArgumentException;
}
