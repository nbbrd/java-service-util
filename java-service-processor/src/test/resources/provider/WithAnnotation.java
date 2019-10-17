package provider;

import nbbrd.service.ServiceProvider;

public class WithAnnotation {

    public interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider2 extends Provider1 {
    }
}
