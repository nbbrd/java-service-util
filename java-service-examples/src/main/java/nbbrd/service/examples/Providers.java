package nbbrd.service.examples;

import nbbrd.service.ServiceProvider;

import java.util.ServiceLoader;

public interface Providers {

    interface FooSPI {
    }

    interface BarSPI {
    }

    // 💡 One provider, one service
    @ServiceProvider
    class FooProvider implements FooSPI {
    }

    // 💡 One provider, multiple services
    @ServiceProvider(FooSPI.class)
    @ServiceProvider(BarSPI.class)
    class FooBarProvider implements FooSPI, BarSPI {
    }

    // 💡 Provider using a static field
    @ServiceProvider
    FooSPI CONSTANT = new FooSPI() {
    };

    // 💡 Provider using a static method
    @ServiceProvider
    static FooSPI getInstance() {
        return new FooSPI() {
        };
    }

    // 💡 Provider using enum values
    @ServiceProvider
    enum CommonFoo implements FooSPI {
        A, B, C
    }

    static void main(String[] args) {
        // 💡 Get all providers for the services
        ServiceLoader.load(FooSPI.class).forEach(System.out::println);
        ServiceLoader.load(BarSPI.class).forEach(System.out::println);
    }
}
