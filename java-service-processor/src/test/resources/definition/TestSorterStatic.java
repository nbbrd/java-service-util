package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

@ServiceDefinition
interface TestSorterStatic {

    @ServiceSorter
    static int getCost() {
        return 0;
    }
}
