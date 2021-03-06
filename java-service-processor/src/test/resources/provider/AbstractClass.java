
import nbbrd.service.ServiceProvider;

class AbstractClass {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    abstract public static class Provider2 extends Provider1 {
    }
}
