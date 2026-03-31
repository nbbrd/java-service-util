package definition;

import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class TestQuantifierMultiple {

    @ServiceDefinition(quantifier = MULTIPLE)
    interface Mutable {
    }
}
