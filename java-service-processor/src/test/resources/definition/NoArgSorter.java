package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

@ServiceDefinition
interface NoArgSorter {

    @ServiceSorter
    int getCost(String arg);
}
