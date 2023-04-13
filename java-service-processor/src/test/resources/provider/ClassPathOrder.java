
import nbbrd.service.ServiceProvider;

class ClassPathOrder {

    interface HelloService {
    }

    @ServiceProvider
    public static class B implements HelloService {
    }

    @ServiceProvider
    public static class C implements HelloService {
    }

    @ServiceProvider
    public static class A implements HelloService {
    }
}
