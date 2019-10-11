package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

@ServiceDefinition
interface StaticSorter {

    @ServiceSorter
    static int getCost() {
        return 0;
    }
}
