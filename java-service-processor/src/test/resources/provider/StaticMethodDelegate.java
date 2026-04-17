package provider;

import nbbrd.service.ServiceProvider;

class StaticMethodDelegate {

    interface HelloService {
        String sayHello();
    }

    static class HelloImpl implements HelloService {
        @Override
        public String sayHello() { return "Hello"; }
    }

    @ServiceProvider
    public static HelloService getInstance() {
        return new HelloImpl();
    }
}


