package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

@ServiceDefinition
interface TestSorterNonComparable {

    @ServiceSorter
    Object getCost();
}
