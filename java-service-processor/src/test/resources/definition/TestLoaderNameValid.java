package definition;

import nbbrd.service.ServiceDefinition;

public class TestLoaderNameValid {

    @ServiceDefinition(loaderName = "internal.FooLoader")
    public interface FooService {
    }

    @ServiceDefinition()
    public interface BarService {
    }
}
