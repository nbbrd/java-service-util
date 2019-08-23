
import nbbrd.service.ServiceProvider;

class WithRepeatedAnnotation {

    interface HelloService {
    }

    interface SomeService {
    }

    @ServiceProvider
    public static class SimpleProvider implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    @ServiceProvider(SomeService.class)
    public static class MultiProvider implements HelloService, SomeService {
    }
}
