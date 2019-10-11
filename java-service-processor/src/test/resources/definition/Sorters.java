package definition;

import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceSorter;

class Sorters {

    @ServiceDefinition
    interface IntSorter {

        @ServiceSorter
        int getCost();
    }

    @ServiceDefinition
    interface LongSorter {

        @ServiceSorter
        long getCost();
    }

    @ServiceDefinition
    interface DoubleSorter {

        @ServiceSorter
        double getCost();
    }

    @ServiceDefinition
    interface ComparableSorter {

        @ServiceSorter
        String getCost();
    }

    @ServiceDefinition
    interface MultiSorter {

        @ServiceSorter
        int getCost();

        @ServiceSorter
        double getAccuracy();
    }

    @ServiceDefinition
    interface ReversedSorter {

        @ServiceSorter(reverse = true)
        int getCost();
    }

    @ServiceDefinition
    interface MultiSorterWithPosition {

        @ServiceSorter(position = 2)
        int getCost();

        @ServiceSorter(position = 1)
        double getAccuracy();
    }
}
