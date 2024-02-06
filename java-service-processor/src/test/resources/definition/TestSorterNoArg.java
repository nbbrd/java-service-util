package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

@ServiceDefinition
interface TestSorterNoArg {

    @ServiceSorter
    int getCost(String arg);
}
