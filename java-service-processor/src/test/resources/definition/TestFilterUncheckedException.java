package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

import java.io.IOException;

@ServiceDefinition
interface TestFilterUncheckedException {

    @ServiceFilter
    boolean isAvailable() throws IllegalArgumentException;
}
