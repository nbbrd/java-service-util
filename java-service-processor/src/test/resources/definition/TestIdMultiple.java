package definition;

import nbbrd.service.*;

@ServiceDefinition(quantifier = Quantifier.MULTIPLE)
interface TestIdMultiple {

    @ServiceId
    String getName();
}

