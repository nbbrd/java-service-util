package definition;

import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

@ServiceDefinition(quantifier = SINGLE, noFallback = true)
interface TestFallbackUnexpected {

}
