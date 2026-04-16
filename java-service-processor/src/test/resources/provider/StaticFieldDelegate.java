package provider;

import nbbrd.service.ServiceProvider;

class StaticFieldDelegate {

    interface HelloService {
        String sayHello();
    }

    static class HelloImpl implements HelloService {
        @Override
        public String sayHello() { return "Hello"; }
    }

    @ServiceProvider
    public static final HelloService INSTANCE = new HelloImpl();
}

