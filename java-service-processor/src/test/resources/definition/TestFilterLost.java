package definition;

import nbbrd.service.ServiceFilter;

interface TestFilterLost {

    @ServiceFilter
    boolean isAvailable();
}
