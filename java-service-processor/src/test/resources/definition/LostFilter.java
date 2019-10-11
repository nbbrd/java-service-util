package definition;

import nbbrd.service.ServiceFilter;

interface LostFilter {

    @ServiceFilter
    boolean isAvailable();
}
