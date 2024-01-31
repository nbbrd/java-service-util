package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

import java.io.IOException;

@ServiceDefinition
interface TestSorterCheckedException {

    @ServiceSorter
    int getCost() throws IOException;
}
