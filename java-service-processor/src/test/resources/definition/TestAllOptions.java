package definition;

import nbbrd.service.*;

@ServiceDefinition(
        quantifier = Quantifier.MULTIPLE,
        batch = true,
        mutability = Mutability.CONCURRENT,
        singleton = true
)
interface TestAllOptions {

    @ServiceId( pattern = ServiceId.SCREAMING_SNAKE_CASE )
    String getName();

    @ServiceSorter(position = 1, reverse = false)
    int getCost1();

    @ServiceSorter(position = 2, reverse = true)
    int getCost2();

    @ServiceFilter(position = 1, negate = false)
    boolean isAvailable();

    @ServiceFilter(position = 2, negate = true)
    boolean isDisabled();
}
