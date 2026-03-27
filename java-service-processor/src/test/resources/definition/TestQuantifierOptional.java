package definition;

import static nbbrd.service.Mutability.*;
import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class TestQuantifierOptional {

    @ServiceDefinition(quantifier = OPTIONAL)
    interface Mutable {
    }
}
