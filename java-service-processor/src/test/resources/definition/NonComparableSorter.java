package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

@ServiceDefinition
interface NonComparableSorter {

    @ServiceSorter
    Object getCost();
}
