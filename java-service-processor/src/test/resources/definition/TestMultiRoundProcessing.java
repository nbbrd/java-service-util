package definition;

import nbbrd.service.ServiceDefinition;

public class TestMultiRoundProcessing {

    @ServiceDefinition(loaderName = "internal.FirstLoader")
    public interface FirstService {
    }

    @ServiceDefinition(loaderName = "internal.SecondLoader")
    public interface SecondService {
        internal.FirstLoader getFirstLoader();
    }
}
