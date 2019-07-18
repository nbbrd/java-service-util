
import nbbrd.service.ServiceProvider;

class WithRepeatedAnnotation {

    interface HelloService {
    }

    interface SomeService {
    }

    @ServiceProvider(HelloService.class)
    @ServiceProvider(SomeService.class)
    public static class Provider1 implements HelloService, SomeService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider2 extends Provider1 {
    }
}
