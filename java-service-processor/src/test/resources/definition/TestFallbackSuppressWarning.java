package definition;

import static nbbrd.service.Quantifier.*;
import nbbrd.service.ServiceDefinition;

@SuppressWarnings(ServiceDefinition.SINGLE_FALLBACK_NOT_EXPECTED)
@ServiceDefinition(quantifier = SINGLE)
interface TestFallbackSuppressWarning {

}
