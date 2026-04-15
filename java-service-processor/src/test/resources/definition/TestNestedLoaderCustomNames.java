package definition;

import nbbrd.service.ServiceDefinition;

public class TestNestedLoaderCustomNames {

    @ServiceDefinition(loaderName = "definition.{{topLevelClassName}}Loader")
    public interface FooService {
    }

    @ServiceDefinition(loaderName = "definition.{{topLevelClassName}}Loader")
    public interface BarService {
    }
}
