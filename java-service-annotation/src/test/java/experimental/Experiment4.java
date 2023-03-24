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

    static class Example {

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
    }

    static class Generated {

        static final class WinRegistryLoader {

            public static Optional<Example.WinRegistry> load() {
                return builder().build().get();
            }

            public static Builder<ServiceLoader<?>> builder() {
                return new Builder<ServiceLoader<?>>(ServiceLoader::load)
                        .streamerOfIterable(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static Builder<ServiceLoader<?>> builder(ClassLoader classLoader) {
                return new Builder<ServiceLoader<?>>(service -> ServiceLoader.load(service, classLoader))
                        .streamerOfIterable(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static <BACKEND> Builder<BACKEND> builder(Function<Class<?>, BACKEND> factory) {
                return new Builder<>(factory);
            }

            public static final class Builder<BACKEND> {

                private final Function<Class<?>, BACKEND> factory;
                private Function<BACKEND, Stream<?>> streamer;
                private Consumer<BACKEND> reloader;

                private Builder(Function<Class<?>, BACKEND> factory) {
                    this.factory = factory;
                }

                public Builder<BACKEND> streamerOfIterable(Function<BACKEND, ? extends Iterable<?>> extractor) {
                    return streamer(backend -> StreamSupport.stream(extractor.apply(backend).spliterator(), false));
                }

                public Builder<BACKEND> streamer(Function<BACKEND, Stream<?>> streamer) {
                    this.streamer = streamer;
                    return this;
                }

                public Builder<BACKEND> reloader(Consumer<BACKEND> reloader) {
                    this.reloader = reloader;
                    return this;
                }

                public WinRegistryLoader build() {
                    Objects.requireNonNull(factory);
                    Objects.requireNonNull(streamer);
                    Objects.requireNonNull(reloader);
                    return new WinRegistryLoader(
                            factory.apply(Example.WinRegistry.class),
                            backend -> streamer.apply((BACKEND) backend),
                            backend -> reloader.accept((BACKEND) backend));
                }
            }

            public Optional<Example.WinRegistry> get() {
                return rawStream().findFirst();
            }

            private Stream<Example.WinRegistry> rawStream() {
                return streamer.apply(source).map(Example.WinRegistry.class::cast);
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

            public static Example.Configuration load() {
                return builder().build().get();
            }


            public static Builder<ServiceLoader<?>> builder() {
                return new Builder<ServiceLoader<?>>(ServiceLoader::load)
                        .streamerOfIterable(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static Builder<ServiceLoader<?>> builder(ClassLoader classLoader) {
                return new Builder<ServiceLoader<?>>(service -> ServiceLoader.load(service, classLoader))
                        .streamerOfIterable(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static <BACKEND> Builder<BACKEND> builder(Function<Class<?>, BACKEND> factory) {
                return new Builder<>(factory);
            }

            public static final class Builder<BACKEND> {

                private final Function<Class<?>, BACKEND> factory;
                private Function<BACKEND, Stream<?>> streamer;
                private Consumer<BACKEND> reloader;

                private Builder(Function<Class<?>, BACKEND> factory) {
                    this.factory = factory;
                }

                public Builder<BACKEND> streamerOfIterable(Function<BACKEND, ? extends Iterable<?>> extractor) {
                    return streamer(backend -> StreamSupport.stream(extractor.apply(backend).spliterator(), false));
                }

                public Builder<BACKEND> streamer(Function<BACKEND, Stream<?>> streamer) {
                    this.streamer = streamer;
                    return this;
                }

                public Builder<BACKEND> reloader(Consumer<BACKEND> reloader) {
                    this.reloader = reloader;
                    return this;
                }

                public ConfigurationLoader build() {
                    Objects.requireNonNull(factory);
                    Objects.requireNonNull(streamer);
                    Objects.requireNonNull(reloader);
                    return new ConfigurationLoader(
                            factory.apply(Example.Configuration.class),
                            backend -> streamer.apply((BACKEND) backend),
                            backend -> reloader.accept((BACKEND) backend));
                }
            }

            public Example.Configuration get() {
                return rawStream().findFirst().orElseThrow(RuntimeException::new);
            }

            private Stream<Example.Configuration> rawStream() {
                return streamer.apply(source).map(Example.Configuration.class::cast);
            }

            public void reload() {
                reloader.accept(source);
            }

            private final Object source;
            private final Function<Object, Stream<?>> streamer;
            private final Consumer<Object> reloader;

            private ConfigurationLoader(Object source, Function<Object, Stream<?>> streamer, Consumer<Object> reloader) {
                this.source = source;
                this.streamer = streamer;
                this.reloader = reloader;
            }
        }

        interface SwingColorSchemeBatch {
            Stream<Example.SwingColorScheme> getProviders();
        }

        static final class SwingColorSchemeLoader {

            public static List<Example.SwingColorScheme> load() {
                return builder().build().get();
            }

            public static Builder<ServiceLoader<?>> builder() {
                return new Builder<ServiceLoader<?>>(ServiceLoader::load)
                        .streamerOfIterable(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static Builder<ServiceLoader<?>> builder(ClassLoader classLoader) {
                return new Builder<ServiceLoader<?>>(service -> ServiceLoader.load(service, classLoader))
                        .streamerOfIterable(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static <BACKEND> Builder<BACKEND> builder(Function<Class<?>, BACKEND> factory) {
                return new Builder<>(factory);
            }

            public static final class Builder<BACKEND> {

                private final Function<Class<?>, BACKEND> factory;
                private Function<BACKEND, Stream<?>> streamer;
                private Consumer<BACKEND> reloader;

                private Builder(Function<Class<?>, BACKEND> factory) {
                    this.factory = factory;
                }

                public Builder<BACKEND> streamerOfIterable(Function<BACKEND, ? extends Iterable<?>> extractor) {
                    return streamer(backend -> StreamSupport.stream(extractor.apply(backend).spliterator(), false));
                }

                public Builder<BACKEND> streamer(Function<BACKEND, Stream<?>> streamer) {
                    this.streamer = streamer;
                    return this;
                }

                public Builder<BACKEND> reloader(Consumer<BACKEND> reloader) {
                    this.reloader = reloader;
                    return this;
                }

                public SwingColorSchemeLoader build() {
                    Objects.requireNonNull(factory);
                    Objects.requireNonNull(streamer);
                    Objects.requireNonNull(reloader);
                    return new SwingColorSchemeLoader(
                            factory.apply(Example.SwingColorScheme.class), factory.apply(SwingColorSchemeBatch.class),
                            backend -> streamer.apply((BACKEND) backend),
                            backend -> reloader.accept((BACKEND) backend));
                }
            }

            public List<Example.SwingColorScheme> get() {
                return rawStream().collect(Collectors.toList());
            }

            private Stream<Example.SwingColorScheme> rawStream() {
                return Stream.concat(
                        streamer.apply(source).map(Example.SwingColorScheme.class::cast),
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

            public static Optional<Example.Messenger> load() {
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

            public Optional<Example.Messenger> get() {
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

            private final AtomicReference<Optional<Example.Messenger>> resource = new AtomicReference<>(delegate.get());

            public Optional<Example.Messenger> get() {
                return resource.get();
            }

            public void set(Optional<Example.Messenger> newValue) {
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

    static class Demo {

        public static void main(String[] args) {
            normalUsage();
            fluentUsage();
            convenientShortcut();
            netBeansLookupBackend();
            customClassLoaderBackend();
            serviceDefinitionAdapter();
        }

        private static void normalUsage() {
            WinRegistryLoader optionalLoader = WinRegistryLoader.builder().build();
            Optional<Example.WinRegistry> optional = optionalLoader.get();
            optional.ifPresent(Demo::printWindowsVersion);
            optionalLoader.reload();

            ConfigurationLoader singleLoader = ConfigurationLoader.builder().build();
            Example.Configuration single = singleLoader.get();
            printProperties(single);
            singleLoader.reload();

            SwingColorSchemeLoader multipleLoader = SwingColorSchemeLoader.builder().build();
            List<Example.SwingColorScheme> multiple = multipleLoader.get();
            multiple.forEach(Demo::printColors);
            multipleLoader.reload();
        }

        private static void fluentUsage() {
            WinRegistryLoader.builder().build().get().ifPresent(Demo::printWindowsVersion);

            printProperties(ConfigurationLoader.builder().build().get());

            SwingColorSchemeLoader.builder().build().get().forEach(Demo::printColors);
        }

        private static void convenientShortcut() {
            WinRegistryLoader.load().ifPresent(Demo::printWindowsVersion);

            printProperties(ConfigurationLoader.load());

            SwingColorSchemeLoader.load().forEach(Demo::printColors);
        }

        private static void netBeansLookupBackend() {
            WinRegistryLoader
                    .builder(Lookup.getDefault()::lookupResult)
                    .streamerOfIterable(Lookup.Result::allInstances)
                    .build()
                    .get().ifPresent(Demo::printWindowsVersion);

            printProperties(
                    ConfigurationLoader
                            .builder(Lookup.getDefault()::lookupResult)
                            .streamerOfIterable(Lookup.Result::allInstances)
                            .build()
                            .get()
            );

            SwingColorSchemeLoader
                    .builder(Lookup.getDefault()::lookupResult)
                    .streamerOfIterable(Lookup.Result::allInstances)
                    .build()
                    .get().forEach(Demo::printColors);
        }

        private static void customClassLoaderBackend() {
            WinRegistryLoader
                    .builder(Thread.currentThread().getContextClassLoader())
                    .build()
                    .get().ifPresent(Demo::printWindowsVersion);

            printProperties(
                    ConfigurationLoader
                            .builder(Thread.currentThread().getContextClassLoader())
                            .build()
                            .get()
            );

            SwingColorSchemeLoader
                    .builder(Thread.currentThread().getContextClassLoader())
                    .build()
                    .get().forEach(Demo::printColors);
        }

        private static void serviceDefinitionAdapter() {
            MessengerLoader optionalAdapter = new MessengerLoader();
            optionalAdapter.set(Optional.empty());
            optionalAdapter.reload();
            optionalAdapter.reset();
            optionalAdapter.get();
        }

        private static void printWindowsVersion(Example.WinRegistry reg) {
            System.out.println(reg.readString(Example.WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName"));
        }

        private static void printProperties(Example.Configuration configuration) {
            configuration.getProperties().forEach((k, v) -> System.out.println(k + "=" + v));
        }

        private static void printColors(Example.SwingColorScheme colorScheme) {
            System.out.println(colorScheme.getColors());
        }
    }
}
