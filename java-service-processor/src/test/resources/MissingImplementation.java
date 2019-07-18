
import nbbrd.service.ServiceProvider;

class MissingImplementation {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider2 {
    }
}
