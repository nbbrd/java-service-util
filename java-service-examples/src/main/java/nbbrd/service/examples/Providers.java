package nbbrd.service.examples;

import nbbrd.service.ServiceProvider;

import java.util.ServiceLoader;

public class Providers {

    public interface FooSPI {
    }

    public interface BarSPI {
    }

    // 💡 One provider, one service
    @ServiceProvider
    public static class FooProvider implements FooSPI {
    }

    // 💡 One provider, multiple services
    @ServiceProvider(FooSPI.class)
    @ServiceProvider(BarSPI.class)
    public static class FooBarProvider implements FooSPI, BarSPI {
    }

    public static void main(String[] args) {
        // 💡 Get all providers for the services
        ServiceLoader.load(FooSPI.class).forEach(System.out::println);
        ServiceLoader.load(BarSPI.class).forEach(System.out::println);
    }
}
