
import nbbrd.service.ServiceProvider;

class StaticInnerClass {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    public class Provider2 extends Provider1 {
    }
}
