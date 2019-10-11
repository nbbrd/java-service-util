package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceFilter;

class Filters {

    @ServiceDefinition
    interface SingleFilter {

        @ServiceFilter
        boolean isAvailable();
    }

    @ServiceDefinition
    interface MultiFilter {

        @ServiceFilter
        boolean isAvailable();

        @ServiceFilter
        boolean isFastEnough();
    }

    @ServiceDefinition
    interface ReversedFilter {

        @ServiceFilter(negate = true)
        boolean isAvailable();
    }

    @ServiceDefinition
    interface MultiFilterWithPosition {

        @ServiceFilter(position = 2)
        boolean isAvailable();

        @ServiceFilter(position = 1)
        boolean isFastEnough();
    }
}
