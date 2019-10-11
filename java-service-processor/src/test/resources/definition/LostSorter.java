package definition;

import nbbrd.service.ServiceSorter;

interface LostSorter {

    @ServiceSorter
    int getCost();
}
