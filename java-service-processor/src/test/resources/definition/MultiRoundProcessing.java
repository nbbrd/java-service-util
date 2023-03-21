package definition;

import nbbrd.service.ServiceDefinition;

public class MultiRoundProcessing {

    @ServiceDefinition(loaderName = "internal.FirstLoader")
    public interface FirstService {
    }

    @ServiceDefinition(loaderName = "internal.SecondLoader")
    public interface SecondService {
        internal.FirstLoader getFirstLoader();
    }
}
