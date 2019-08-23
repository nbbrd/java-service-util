
import java.io.Serializable;
import nbbrd.service.ServiceProvider;

class VoidService {

    interface HelloService {
    }

    @ServiceProvider
    public static class Provider1 implements HelloService, Serializable {
    }
}
