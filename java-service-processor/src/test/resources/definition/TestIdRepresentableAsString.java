package definition;

import nbbrd.design.RepresentableAsString;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;

@ServiceDefinition(quantifier = Quantifier.MULTIPLE)
interface TestIdRepresentableAsString {

    @RepresentableAsString(parseMethodName = "parse", formatMethodName = "serialize")
    interface Version {
        static Version parse(CharSequence s) { throw new UnsupportedOperationException(); }
        String serialize();
    }

    // 💡 @RepresentableAsString.formatMethodName="serialize" is picked up automatically
    @ServiceId
    Version getVersion();
}


