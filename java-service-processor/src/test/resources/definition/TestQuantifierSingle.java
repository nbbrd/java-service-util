package definition;

import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

class TestQuantifierSingle {

    @ServiceDefinition(quantifier = SINGLE)
    interface Mutable {
    }
}
