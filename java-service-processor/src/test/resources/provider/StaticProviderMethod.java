
import nbbrd.service.ServiceProvider;

class StaticProviderMethod {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {

        private Provider1() {
        }

        public static Provider1 provider() {
            return new Provider1();
        }
    }

    @ServiceProvider(HelloService.class)
    public static class Provider2 implements HelloService {

        private Provider2() {
        }

        public static HelloService provider() {
            return new Provider1();
        }
    }
}
