package definition;

import nbbrd.service.ServiceDefinition;

public class AlternateNames {

    @ServiceDefinition(loaderName = "internal.FooLoader", batch = true)
    public interface FooService {
    }

    @ServiceDefinition(batchName = "internal.BarBatch", batch = true)
    public interface BarService {
    }
}
