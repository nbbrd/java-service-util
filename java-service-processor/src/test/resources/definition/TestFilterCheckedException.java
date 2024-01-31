package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

import java.io.IOException;

@ServiceDefinition
interface TestFilterCheckedException {

    @ServiceFilter
    boolean isAvailable() throws IOException;
}
