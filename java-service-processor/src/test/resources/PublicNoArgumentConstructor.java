
import nbbrd.service.ServiceProvider;

class PublicNoArgumentConstructor {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider2 extends Provider1 {

        protected Provider2() {
        }
    }
}
