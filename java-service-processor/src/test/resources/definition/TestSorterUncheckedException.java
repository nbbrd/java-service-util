package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

import java.io.IOException;

@ServiceDefinition
interface TestSorterUncheckedException {

    @ServiceSorter
    int getCost() throws IllegalArgumentException;
}
