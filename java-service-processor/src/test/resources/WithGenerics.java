
import nbbrd.service.ServiceProvider;

class WithGenerics {

    interface HelloService<X, Y> {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider2 implements HelloService<String, Integer> {
    }
}
