
import nbbrd.service.ServiceProvider;

class DuplicatedAnnotation {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider2 extends Provider1 {
    }
}
