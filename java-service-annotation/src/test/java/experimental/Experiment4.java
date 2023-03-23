package experimental;

import experimental.Experiment4.Generated.ConfigurationLoader;
import experimental.Experiment4.Generated.MessengerLoader;
import experimental.Experiment4.Generated.SwingColorSchemeLoader;
import experimental.Experiment4.Generated.WinRegistryLoader;
import nbbrd.service.Mutability;
import org.openide.util.Lookup;

import java.awt.*;
import java.lang.annotation.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Experiment4 {

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OptionalService {

        /**
         * Specifies the name of the loader. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        ClassName loaderName() default @ClassName;
    }

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SingleService {

        /**
         * Specifies the name of the loader. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        ClassName loaderName() default @ClassName;

        /**
         * Specifies the fallback class to use if no service is available.
         * <p>
         * Requirements:
         * <ul>
         * <li>must be assignable to the service type
         * <li>must be instantiable either by constructor, static method, enum field
         * or static final field
         * </ul>
         *
         * @return the fallback class
         */
        Class<?> fallback();
    }

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MultipleService {

        /**
         * Specifies the name of the loader. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        ClassName loaderName() default @ClassName;

        /**
         * Specifies the name of the batch loading. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        ClassName batchName() default @ClassName;

        /**
         * Specifies if batch loading is disabled.
         *
         * @return true if batch loading is disabled, false otherwise
         */
        boolean noBatch() default false;
    }

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceDefinitionAdapter {

        /**
         * Specifies the name of the adapter. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        ClassName adapterName() default @ClassName;

        /**
         * Specifies the mutability of the adapter.
         *
         * @return a non-null mutability
         */
        Mutability mutability() default Mutability.NONE;

        /**
         * Specifies if the adapter must be a singleton.
         *
         * @return true if the loader is a singleton, false otherwise
         */
        boolean singleton() default false;
    }

    static class Demo {

        @OptionalService
        interface WinRegistry {
            String readString(int hkey, String key, String valueName);

            int HKEY_LOCAL_MACHINE = 0;
        }

        @SingleService(fallback = Configuration.SystemProperties.class)
        interface Configuration {
            Properties getProperties();

            class SystemProperties implements Configuration {

                @Override
                public Properties getProperties() {
                    return System.getProperties();
                }
            }
        }

        @MultipleService
        interface SwingColorScheme {
            List<Color> getColors();
        }

        @OptionalService(loaderName = @ClassName(simpleName = "MessengerService"))
        @ServiceDefinitionAdapter(adapterName = @ClassName(simpleName = "MessengerLoader"), mutability = Mutability.CONCURRENT)
        public interface Messenger {
            void send(String message);
        }


        public static void main(String[] args) {
            // Normal usage
            {
                WinRegistryLoader optionalLoader = WinRegistryLoader.ofServiceLoader();
                Optional<WinRegistry> optional = optionalLoader.get();
                optional.ifPresent(Demo::printWindowsVersion);
                optionalLoader.reload();

                ConfigurationLoader singleLoader = ConfigurationLoader.ofServiceLoader();
                Configuration single = singleLoader.get();
                printProperties(single);
                singleLoader.reload();

                SwingColorSchemeLoader multipleLoader = SwingColorSchemeLoader.ofServiceLoader();
                List<SwingColorScheme> multiple = multipleLoader.get();
                multiple.forEach(Demo::printColors);
                multipleLoader.reload();
            }

            // Fluent usage
            {
                WinRegistryLoader.ofServiceLoader().get().ifPresent(Demo::printWindowsVersion);

                printProperties(ConfigurationLoader.ofServiceLoader().get());

                SwingColorSchemeLoader.ofServiceLoader().get().forEach(Demo::printColors);
            }

            // Convenient shortcut
            {
                WinRegistryLoader.load().ifPresent(Demo::printWindowsVersion);

                printProperties(ConfigurationLoader.load());

                SwingColorSchemeLoader.load().forEach(Demo::printColors);
            }

            // NetBeans Lookup backend
            {
                WinRegistryLoader.of(Lookup.getDefault()::lookupResult, Lookup.Result::allInstances, Demo::doNothing)
                        .get().ifPresent(Demo::printWindowsVersion);
            }

            // Custom ClassLoader backend
            {
                WinRegistryLoader.of(service -> ServiceLoader.load(service, Thread.currentThread().getContextClassLoader()), backend -> backend, ServiceLoader::reload)
                        .get().ifPresent(Demo::printWindowsVersion);
            }

            // @ServiceDefinition adapter
            {
                MessengerLoader optionalAdapter = new MessengerLoader();
                optionalAdapter.set(Optional.empty());
                optionalAdapter.reload();
                optionalAdapter.reset();
                optionalAdapter.get();
            }
        }

        private static void printWindowsVersion(WinRegistry reg) {
            System.out.println(reg.readString(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName"));
        }

        private static void printProperties(Configuration configuration) {
            configuration.getProperties().forEach((k, v) -> System.out.println(k + "=" + v));
        }

        private static void printColors(SwingColorScheme colorScheme) {
            System.out.println(colorScheme.getColors());
        }

        private static void doNothing(Object o) {

        }
    }

    static class Generated {

        static final class WinRegistryLoader {

            public static Optional<Demo.WinRegistry> load() {
                return ofServiceLoader().get();
            }

            public static WinRegistryLoader ofServiceLoader() {
                return of(ServiceLoader::load, backend -> backend, ServiceLoader::reload);
            }

            public static <BACKEND> WinRegistryLoader of(
                    Function<Class<?>, BACKEND> factory,
                    Function<BACKEND, ? extends Iterable<?>> streamer,
                    Consumer<BACKEND> reloader
            ) {
                return new WinRegistryLoader(
                        factory.apply(Demo.WinRegistry.class),
                        backend -> StreamSupport.stream(streamer.apply((BACKEND) backend).spliterator(), false),
                        backend -> reloader.accept((BACKEND) backend)
                );
            }

            public Optional<Demo.WinRegistry> get() {
                return rawStream().findFirst();
            }

            private Stream<Demo.WinRegistry> rawStream() {
                return streamer.apply(source).map(Demo.WinRegistry.class::cast);
            }

            public void reload() {
                reloader.accept(source);
            }

            private final Object source;
            private final Function<Object, Stream<?>> streamer;
            private final Consumer<Object> reloader;

            private WinRegistryLoader(Object source, Function<Object, Stream<?>> streamer, Consumer<Object> reloader) {
                this.source = source;
                this.streamer = streamer;
                this.reloader = reloader;
            }
        }

        static final class ConfigurationLoader {

            public static Demo.Configuration load() {
                return ofServiceLoader().get();
            }

            public static ConfigurationLoader ofServiceLoader() {
                return of(ServiceLoader::load, ServiceLoader::reload);
            }

            public static <BACKEND extends Iterable<?>> ConfigurationLoader of(Function<Class<?>, BACKEND> backend, Consumer<BACKEND> reloader) {
                throw new UnsupportedOperationException();
            }

            public Demo.Configuration get() {
                throw new UnsupportedOperationException();
            }

            public void reload() {
                throw new UnsupportedOperationException();
            }

            private ConfigurationLoader() {
                throw new UnsupportedOperationException();
            }
        }

        interface SwingColorSchemeBatch {
            Stream<Demo.SwingColorScheme> getProviders();
        }

        static final class SwingColorSchemeLoader {

            public static List<Demo.SwingColorScheme> load() {
                return ofServiceLoader().get();
            }

            public static SwingColorSchemeLoader ofServiceLoader() {
                return of(ServiceLoader::load, backend -> backend, ServiceLoader::reload);
            }

            public static <BACKEND> SwingColorSchemeLoader of(
                    Function<Class<?>, BACKEND> factory,
                    Function<BACKEND, ? extends Iterable<?>> streamer,
                    Consumer<BACKEND> reloader
            ) {
                return new SwingColorSchemeLoader(
                        factory.apply(Demo.WinRegistry.class), factory.apply(SwingColorSchemeBatch.class),
                        backend -> StreamSupport.stream(streamer.apply((BACKEND) backend).spliterator(), false),
                        backend -> reloader.accept((BACKEND) backend)
                );
            }

            public List<Demo.SwingColorScheme> get() {
                return rawStream().collect(Collectors.toList());
            }

            private Stream<Demo.SwingColorScheme> rawStream() {
                return Stream.concat(
                        streamer.apply(source).map(Demo.SwingColorScheme.class::cast),
                        streamer.apply(batch).map(SwingColorSchemeBatch.class::cast).flatMap(SwingColorSchemeBatch::getProviders)
                );
            }

            public void reload() {
                reloader.accept(source);
                reloader.accept(batch);
            }

            private final Object source;
            private final Object batch;
            private final Function<Object, Stream<?>> streamer;
            private final Consumer<Object> reloader;

            private SwingColorSchemeLoader(Object source, Object batch, Function<Object, Stream<?>> streamer, Consumer<Object> reloader) {
                this.source = source;
                this.batch = batch;
                this.streamer = streamer;
                this.reloader = reloader;
            }
        }

        static final class MessengerService {

            public static Optional<Demo.Messenger> load() {
                return ofServiceLoader().get();
            }

            public static MessengerService ofServiceLoader() {
                return of(ServiceLoader::load, backend -> backend, ServiceLoader::reload);
            }

            public static <BACKEND> MessengerService of(
                    Function<Class<?>, BACKEND> factory,
                    Function<BACKEND, ? extends Iterable<?>> streamer,
                    Consumer<BACKEND> reloader
            ) {
                throw new UnsupportedOperationException();
            }

            public Optional<Demo.Messenger> get() {
                throw new UnsupportedOperationException();
            }

            public void reload() {
                throw new UnsupportedOperationException();
            }

            private MessengerService() {
                throw new UnsupportedOperationException();
            }
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        static final class MessengerLoader {

            private final MessengerService delegate = MessengerService.ofServiceLoader();

            private final AtomicReference<Optional<Demo.Messenger>> resource = new AtomicReference<>(delegate.get());

            public Optional<Demo.Messenger> get() {
                return resource.get();
            }

            public void set(Optional<Demo.Messenger> newValue) {
                resource.set(Objects.requireNonNull(newValue));
            }

            public void reload() {
                synchronized (delegate) {
                    delegate.reload();
                    set(delegate.get());
                }
            }

            public void reset() {
                synchronized (delegate) {
                    set(delegate.get());
                }
            }
        }
    }
}
