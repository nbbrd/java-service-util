
import nbbrd.service.ServiceProvider;

class InferredService {

    interface HelloService {
    }

    @ServiceProvider
    public static class Provider1 implements HelloService {
    }
}
