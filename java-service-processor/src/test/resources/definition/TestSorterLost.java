package definition;

import nbbrd.service.ServiceSorter;

interface TestSorterLost {

    @ServiceSorter
    int getCost();
}
