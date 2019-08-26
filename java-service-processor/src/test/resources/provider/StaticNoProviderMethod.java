
import nbbrd.service.ServiceProvider;

class StaticNoProviderMethod {

    interface HelloService {
    }

    @ServiceProvider(HelloService.class)
    public static class Provider implements HelloService {

        private Provider() {
        }

        public static Provider stuff() {
            return new Provider();
        }
    }
}
