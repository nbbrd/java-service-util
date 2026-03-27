package experimental;

import experimental.Experiment5.Generated.ConfigurationLoader;
import experimental.Experiment5.Generated.SwingColorSchemeLoader;
import experimental.Experiment5.Generated.WinRegistryLoader;
import nbbrd.service.Quantifier;
import org.jspecify.annotations.NonNull;
import org.openide.util.Lookup;

import java.awt.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public class Experiment5 {

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceDefinition {

        /**
         * Specifies how many instances are returned by the loader.
         *
         * @return a non-null quantifier
         */
        @Feature("SINGLE_ANNOTATION")
        Quantifier value() default Quantifier.OPTIONAL;

        /**
         * Specifies the name of the loader. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        @Feature("CUSTOM_NAMING")
        String loaderName() default "";

        /**
         * Specifies the batch class to use during loading.
         * Void class disables batch loading.
         * <p>
         * Requirements:
         * <ul>
         * <li>must be assignable to the service type
         * <li>must be instantiable either by constructor, static method, enum field
         * or static final field
         * </ul>
         *
         * @return a class name
         */
        @Feature("BATCH_LOADING")
        Class<?> batch() default Void.class;

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
        @Feature("NO_SERVICE_FALLBACK")
        Class<?> fallback() default Void.class;
    }

    public static class Example {

        @ServiceDefinition(Quantifier.OPTIONAL)
        public interface WinRegistry {
            String readString(int hkey, String key, String valueName);

            int HKEY_LOCAL_MACHINE = 0;
        }

        @ServiceDefinition(value = Quantifier.SINGLE, fallback = Configuration.SystemProperties.class)
        public interface Configuration {
            Properties getProperties();

            class SystemProperties implements Configuration {

                @Override
                public Properties getProperties() {
                    return System.getProperties();
                }
            }
        }

        @ServiceDefinition(value = Quantifier.MULTIPLE, batch = SwingColorSchemeBatch.class)
        public interface SwingColorScheme {
            List<Color> getColors();
        }

        public interface SwingColorSchemeBatch {
            Stream<SwingColorScheme> getProviders();
        }
    }

    static class Generated {

        @lombok.RequiredArgsConstructor
        abstract static class LoaderTemplate<SERVICE, PROVIDER, BATCH> {

            @Feature("CUSTOM_BACKEND")
            protected final Class<PROVIDER> providerType;
            protected final Iterable<?> providerSource;
            protected final Runnable providerReloader;

            @Feature("CUSTOM_BACKEND")
            protected final Class<BATCH> batchType;
            protected final Iterable<?> batchSource;
            protected final Runnable batchReloader;

            @Feature("BATCH_LOADING")
            protected final Function<BATCH, Stream<PROVIDER>> batchMapper;

            @Feature("ERROR_REPORTING")
            protected final Consumer<Throwable> onUnexpectedError;

            @Feature("PREPROCESSING_PROPERTIES")
            protected final Properties properties;

            @Feature("PREPROCESSING")
            protected final Function<Properties, Predicate<PROVIDER>> filtering;
            protected final Function<Properties, Comparator<PROVIDER>> sorting;
            protected final Function<Properties, Function<PROVIDER, SERVICE>> mapping;

            protected Stream<SERVICE> getAll() {
                return Stream.concat(
                                safeStream(providerSource).filter(providerType::isInstance).map(providerType::cast),
                                safeStream(batchSource).filter(batchType::isInstance).map(batchType::cast).flatMap(batchMapper)
                        )
                        .filter(filtering.apply(properties))
                        .sorted(sorting.apply(properties))
                        .map(mapping.apply(properties));
            }

            @Feature("ERROR_REPORTING")
            private <X> Stream<X> safeStream(Iterable<X> iterable) {
                return new SafeIterable<>(iterable, onUnexpectedError).asStream();
            }

            @Feature("RELOADING")
            public void reload() {
                providerReloader.run();
                batchReloader.run();
            }

            @Feature("RETRIEVAL")
            abstract public Object get();

            @Feature("FLUENT_USAGE")
            abstract static class Builder<SELF extends Builder<SELF>> {

                protected Function<Class<?>, Object> factory = ServiceLoader::load;
                protected Function<Object, Iterable<?>> streamer = backend -> (ServiceLoader<?>) backend;
                protected Consumer<Object> reloader = backend -> ((ServiceLoader<?>) backend).reload();

                protected Consumer<Throwable> onUnexpectedError = Throwable::printStackTrace;
                protected Properties properties = System.getProperties();

                @SuppressWarnings("unchecked")
                private SELF self() {
                    return (SELF) this;
                }

                @SuppressWarnings("unchecked")
                @Feature("CUSTOM_BACKEND")
                public <BACKEND> SELF backend(Function<Class<?>, BACKEND> factory, Function<BACKEND, Iterable<?>> streamer, Consumer<BACKEND> reloader) {
                    this.factory = (Function<Class<?>, Object>) factory;
                    this.streamer = (Function<Object, Iterable<?>>) streamer;
                    this.reloader = (Consumer<Object>) reloader;
                    return self();
                }

                @Feature("ERROR_REPORTING")
                public SELF onUnexpectedError(Consumer<Throwable> onUnexpectedError) {
                    this.onUnexpectedError = onUnexpectedError;
                    return self();
                }

                @Feature("PREPROCESSING_PROPERTIES")
                public SELF properties(Properties properties) {
                    this.properties = properties;
                    return self();
                }
            }
        }

        public static final class WinRegistryLoader extends LoaderTemplate<Example.WinRegistry, Example.WinRegistry, Void> {

            @Feature("CONVENIENT_SHORTCUTS")
            public static Optional<Example.WinRegistry> load() {
                return builder().build().get();
            }

            @Override
            @Feature("RETRIEVAL")
            public Optional<Example.WinRegistry> get() {
                return getAll().findFirst();
            }

            @Feature("FLUENT_USAGE")
            public static Builder builder() {
                return new Builder();
            }

            private WinRegistryLoader(
                    Iterable<?> providerSource, Runnable providerReloader,
                    Iterable<?> batchSource, Runnable batchReloader,
                    Consumer<Throwable> onUnexpectedError, Properties properties) {
                super(
                        Example.WinRegistry.class, providerSource, providerReloader,
                        Void.class, batchSource, batchReloader, noBatchMapper(),
                        onUnexpectedError, properties,
                        noFiltering(), noSorting(), noMapping()
                );
            }

            public static final class Builder extends LoaderTemplate.Builder<Builder> {

                public WinRegistryLoader build() {
                    Object providerBackend = factory.apply(Example.WinRegistry.class);
                    Object batchBackend = factory.apply(Void.class);
                    return new WinRegistryLoader(
                            streamer.apply(providerBackend), () -> reloader.accept(providerBackend),
                            streamer.apply(batchBackend), () -> reloader.accept(batchBackend),
                            onUnexpectedError, properties
                    );
                }
            }
        }

        public static final class ConfigurationLoader extends LoaderTemplate<Example.Configuration, Example.Configuration, Void> {

            @Feature("CONVENIENT_SHORTCUTS")
            public static Example.Configuration load() {
                return builder().build().get();
            }

            @Override
            @Feature("RETRIEVAL")
            public Example.Configuration get() {
                return getAll().findFirst().orElseGet(Example.Configuration.SystemProperties::new);
            }

            @Feature("FLUENT_USAGE")
            public static Builder builder() {
                return new Builder();
            }

            private ConfigurationLoader(
                    Iterable<?> providerSource, Runnable providerReloader,
                    Iterable<?> batchSource, Runnable batchReloader,
                    Consumer<Throwable> onUnexpectedError, Properties properties) {
                super(
                        Example.Configuration.class, providerSource, providerReloader,
                        Void.class, batchSource, batchReloader, noBatchMapper(),
                        onUnexpectedError, properties,
                        noFiltering(), noSorting(), noMapping()
                );
            }

            public static final class Builder extends LoaderTemplate.Builder<Builder> {

                public ConfigurationLoader build() {
                    Object providerBackend = factory.apply(Example.Configuration.class);
                    Object batchBackend = factory.apply(Void.class);
                    return new ConfigurationLoader(
                            streamer.apply(providerBackend), () -> reloader.accept(providerBackend),
                            streamer.apply(batchBackend), () -> reloader.accept(batchBackend),
                            onUnexpectedError, properties
                    );
                }
            }
        }

        public static final class SwingColorSchemeLoader extends LoaderTemplate<Example.SwingColorScheme, Example.SwingColorScheme, Example.SwingColorSchemeBatch> {

            @Feature("CONVENIENT_SHORTCUTS")
            public static List<Example.SwingColorScheme> load() {
                return builder().build().get();
            }

            @Override
            @Feature("RETRIEVAL")
            public List<Example.SwingColorScheme> get() {
                return getAll().collect(toList());
            }

            @Feature("FLUENT_USAGE")
            public static Builder builder() {
                return new Builder();
            }

            private SwingColorSchemeLoader(
                    Iterable<?> providerSource, Runnable providerReloader,
                    Iterable<?> batchSource, Runnable batchReloader,
                    Consumer<Throwable> onUnexpectedError, Properties properties) {
                super(
                        Example.SwingColorScheme.class, providerSource, providerReloader,
                        Example.SwingColorSchemeBatch.class, batchSource, batchReloader, Example.SwingColorSchemeBatch::getProviders,
                        onUnexpectedError, properties,
                        noFiltering(), noSorting(), noMapping()
                );
            }

            public static final class Builder extends LoaderTemplate.Builder<Builder> {

                public SwingColorSchemeLoader build() {
                    Object providerBackend = factory.apply(Example.SwingColorScheme.class);
                    Object batchBackend = factory.apply(Example.SwingColorSchemeBatch.class);
                    return new SwingColorSchemeLoader(
                            streamer.apply(providerBackend), () -> reloader.accept(providerBackend),
                            streamer.apply(batchBackend), () -> reloader.accept(batchBackend),
                            onUnexpectedError, properties
                    );
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
            catchUnexpectedErrors();
            preprocessingProperties();
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
                    .builder()
                    .backend(Lookup.getDefault()::lookupResult, Lookup.Result::allInstances, lookupReload())
                    .build()
                    .get().ifPresent(Demo::printWindowsVersion);

            printProperties(
                    ConfigurationLoader
                            .builder()
                            .backend(Lookup.getDefault()::lookupResult, Lookup.Result::allInstances, lookupReload())
                            .build()
                            .get()
            );

            SwingColorSchemeLoader
                    .builder()
                    .backend(Lookup.getDefault()::lookupResult, Lookup.Result::allInstances, lookupReload())
                    .build()
                    .get().forEach(Demo::printColors);
        }

        private static void customClassLoaderBackend() {
            ClassLoader customClassLoader = Thread.currentThread().getContextClassLoader();

            WinRegistryLoader
                    .builder()
                    .backend(service -> ServiceLoader.load(service, customClassLoader), Function.identity(), o -> ((ServiceLoader<?>) o).reload())
                    .build()
                    .get().ifPresent(Demo::printWindowsVersion);

            printProperties(
                    ConfigurationLoader
                            .builder()
                            .backend(service -> ServiceLoader.load(service, customClassLoader), Function.identity(), o -> ((ServiceLoader<?>) o).reload())
                            .build()
                            .get()
            );

            SwingColorSchemeLoader
                    .builder()
                    .backend(service -> ServiceLoader.load(service, customClassLoader), Function.identity(), o -> ((ServiceLoader<?>) o).reload())
                    .build()
                    .get().forEach(Demo::printColors);
        }

        private static void catchUnexpectedErrors() {
            SwingColorSchemeLoader
                    .builder()
                    .onUnexpectedError(ex -> Logger.getAnonymousLogger().log(Level.WARNING, "While loading", ex))
                    .build()
                    .get()
                    .forEach(Demo::printColors);
        }

        private static void preprocessingProperties() {
            Properties props = new Properties(System.getProperties());
            props.setProperty("enableSystemColorScheme", "true");

            SwingColorSchemeLoader
                    .builder()
                    .properties(props)
                    .build()
                    .get()
                    .forEach(Demo::printColors);
        }

        private static void printWindowsVersion(Example.WinRegistry reg) {
            System.out.println(reg.readString(Example.WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName"));
        }

        private static void printProperties(Example.Configuration configuration) {
            System.out.println(configuration.getProperties().getProperty("java.runtime.version"));
        }

        private static void printColors(Example.SwingColorScheme colorScheme) {
            System.out.println(colorScheme.getColors());
        }
    }

    private static @NonNull Consumer<Lookup.Result<?>> lookupReload() {
        return ignoreBackend -> {
        };
    }

    private static <PROVIDER> Function<Properties, Predicate<PROVIDER>> noFiltering() {
        return ignoreProperties -> ignoreProvider -> true;
    }

    private static <PROVIDER> Function<Properties, Comparator<PROVIDER>> noSorting() {
        return ignoreProperties -> (leftProvider, rightProvider) -> -1;
    }

    private static <PROVIDER> Function<Properties, Function<PROVIDER, PROVIDER>> noMapping() {
        return ignoreProperties -> Function.identity();
    }

    private static <BATCH, PROVIDER> Function<BATCH, Stream<PROVIDER>> noBatchMapper() {
        return ignore -> Stream.empty();
    }
}
