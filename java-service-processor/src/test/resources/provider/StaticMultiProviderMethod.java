
import nbbrd.service.ServiceProvider;

class StaticMultiProviderMethod {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider1 implements HelloService {

        private Provider1() {
        }

        public static Provider1 provider() {
            return new Provider1();
        }

        public static Provider1 stuff() {
            return new Provider1();
        }
    }
}
