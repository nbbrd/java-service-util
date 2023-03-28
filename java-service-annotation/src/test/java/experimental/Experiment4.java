package experimental;

import experimental.Experiment4.Generated.ConfigurationLoader;
import experimental.Experiment4.Generated.SwingColorSchemeLoader;
import experimental.Experiment4.Generated.WinRegistryLoader;
import nbbrd.service.Mutability;
import nbbrd.service.ServiceFilter;
import nbbrd.service.ServiceSorter;
import org.openide.util.Lookup;

import java.awt.*;
import java.io.IOException;
import java.lang.annotation.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
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
        Class<?> batch();
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

        @MultipleService(batch = SwingColorSchemeBatch.class)
        interface SwingColorScheme {
            List<Color> getColors();
        }

        interface SwingColorSchemeBatch {
            Stream<Example.SwingColorScheme> getProviders();
        }

        @MultipleService(batch = Void.class)
        @ServiceDefinitionAdapter(
                adapterName = @ClassName(simpleName = "internal.FileTypeSpiLoader"),
                mutability = Mutability.CONCURRENT,
                singleton = true
        )
        public interface FileTypeSpi {

            enum Accuracy {HIGH, LOW}

            String getContentTypeOrNull(Path file) throws IOException;

            @ServiceSorter
            Accuracy getAccuracy();

            @ServiceFilter
            boolean isAvailable();
        }
    }

    static class Generated {

        static abstract class LoaderTemplate<T, B> {

            protected final Iterable<T> source;

            protected final Runnable sourceReloader;

            protected final Iterable<B> batch;

            protected final Runnable batchReloader;

            protected final Function<B, Iterable<T>> mapper;

            protected final Consumer<Throwable> onUnexpectedError;

            protected LoaderTemplate(
                    Iterable<T> source, Runnable sourceReloader,
                    Iterable<B> batch, Runnable batchReloader,
                    Function<B, Iterable<T>> mapper,
                    Consumer<Throwable> onUnexpectedError
            ) {
                this.source = source;
                this.sourceReloader = sourceReloader;
                this.batch = batch;
                this.batchReloader = batchReloader;
                this.mapper = mapper;
                this.onUnexpectedError = onUnexpectedError;
            }

            protected Stream<T> getAll() {
                return Stream.concat(
                        new SafeIterable<>(source, onUnexpectedError).asStream(),
                        new SafeIterable<>(batch, onUnexpectedError).asStream()
                                .flatMap(value -> new SafeIterable<>(mapper.apply(value), onUnexpectedError).asStream()));
            }

            public void reload() {
                sourceReloader.run();
                batchReloader.run();
            }

            public static <X> Iterable<X> noLoad() {
                return emptyList();
            }

            public static Runnable noReload() {
                return () -> {
                };
            }

            public static <B, T> Function<B, Iterable<T>> noMapping() {
                return ignore -> emptyList();
            }
        }

        static abstract class OptionalTemplate<T> extends LoaderTemplate<T, Void> {

            protected OptionalTemplate(Iterable<T> source, Runnable sourceReloader, Consumer<Throwable> onUnexpectedError) {
                super(source, sourceReloader, noLoad(), noReload(), noMapping(), onUnexpectedError);
            }

            public Optional<T> get() {
                return getAll().findFirst();
            }
        }

        static abstract class SingleTemplate<T> extends LoaderTemplate<T, Void> {

            protected SingleTemplate(Iterable<T> source, Runnable sourceReloader, Consumer<Throwable> onUnexpectedError) {
                super(source, sourceReloader, noLoad(), noReload(), noMapping(), onUnexpectedError);
            }

            public T get() {
                return getAll().findFirst().orElseThrow(RuntimeException::new);
            }
        }

        static abstract class MultipleTemplate<T, B> extends LoaderTemplate<T, B> {

            protected MultipleTemplate(
                    Iterable<T> source, Runnable sourceReloader,
                    Iterable<B> batch, Runnable batchReloader,
                    Function<B, Iterable<T>> mapper,
                    Consumer<Throwable> onUnexpectedError
            ) {
                super(source, sourceReloader, batch, batchReloader, mapper, onUnexpectedError);
            }

            public List<T> get() {
                return getAll().collect(Collectors.toList());
            }
        }

        @SuppressWarnings("unchecked")
        static abstract class BuilderTemplate<BACKEND, BUILDER extends BuilderTemplate<BACKEND, BUILDER>> {

            protected final Function<Class<?>, BACKEND> factory;
            protected Function<BACKEND, Iterable<?>> streamer;
            protected Consumer<BACKEND> reloader;
            protected Consumer<Throwable> onUnexpectedError = Throwable::printStackTrace;

            protected BuilderTemplate(Function<Class<?>, BACKEND> factory) {
                this.factory = factory;
            }

            public BUILDER streamer(Function<BACKEND, Iterable<?>> streamer) {
                this.streamer = streamer;
                return (BUILDER) this;
            }

            public BUILDER reloader(Consumer<BACKEND> reloader) {
                this.reloader = reloader;
                return (BUILDER) this;
            }

            public BUILDER onUnexpectedError(Consumer<Throwable> onUnexpectedError) {
                this.onUnexpectedError = onUnexpectedError;
                return (BUILDER) this;
            }

            protected <X> Iterable<X> asLoader(Class<X> type, BACKEND backend) {
                return (Iterable<X>) streamer.apply(backend);
            }

            protected Runnable asReloader(BACKEND backend) {
                return () -> reloader.accept(backend);
            }

            protected <B, T> Function<B, Iterable<T>> asMapper(Function<B, Stream<T>> mapper) {
                return mapper.andThen(o -> o::iterator);
            }

            abstract public Object build();
        }

        static final class WinRegistryLoader extends OptionalTemplate<Example.WinRegistry> {

            public static Optional<Example.WinRegistry> load() {
                return builder().build().get();
            }

            public static Builder<ServiceLoader<?>> builder() {
                return new Builder<ServiceLoader<?>>(ServiceLoader::load)
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static Builder<ServiceLoader<?>> builder(ClassLoader classLoader) {
                return new Builder<ServiceLoader<?>>(service -> ServiceLoader.load(service, classLoader))
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static <BACKEND> Builder<BACKEND> builder(Function<Class<?>, BACKEND> factory) {
                return new Builder<>(factory);
            }

            private WinRegistryLoader(Iterable<Example.WinRegistry> source, Runnable sourceReloader, Consumer<Throwable> onUnexpectedError) {
                super(source, sourceReloader, onUnexpectedError);
            }

            public static final class Builder<BACKEND> extends BuilderTemplate<BACKEND, Builder<BACKEND>> {

                private Builder(Function<Class<?>, BACKEND> factory) {
                    super(factory);
                }

                public WinRegistryLoader build() {
                    BACKEND sourceBackend = factory.apply(Example.WinRegistry.class);
                    return new WinRegistryLoader(
                            asLoader(Example.WinRegistry.class, sourceBackend),
                            asReloader(sourceBackend),
                            onUnexpectedError
                    );
                }
            }
        }

        static final class ConfigurationLoader extends SingleTemplate<Example.Configuration> {

            public static Example.Configuration load() {
                return builder().build().get();
            }


            public static Builder<ServiceLoader<?>> builder() {
                return new Builder<ServiceLoader<?>>(ServiceLoader::load)
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static Builder<ServiceLoader<?>> builder(ClassLoader classLoader) {
                return new Builder<ServiceLoader<?>>(service -> ServiceLoader.load(service, classLoader))
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static <BACKEND> Builder<BACKEND> builder(Function<Class<?>, BACKEND> factory) {
                return new Builder<>(factory);
            }

            private ConfigurationLoader(Iterable<Example.Configuration> source, Runnable sourceReloader, Consumer<Throwable> onUnexpectedError) {
                super(source, sourceReloader, onUnexpectedError);
            }

            public static final class Builder<BACKEND> extends BuilderTemplate<BACKEND, Builder<BACKEND>> {

                private Builder(Function<Class<?>, BACKEND> factory) {
                    super(factory);
                }

                public ConfigurationLoader build() {
                    BACKEND sourceBackend = factory.apply(Example.Configuration.class);
                    return new ConfigurationLoader(
                            asLoader(Example.Configuration.class, sourceBackend),
                            asReloader(sourceBackend),
                            onUnexpectedError
                    );
                }
            }
        }

        static final class SwingColorSchemeLoader extends MultipleTemplate<Example.SwingColorScheme, Example.SwingColorSchemeBatch> {

            public static List<Example.SwingColorScheme> load() {
                return builder().build().get();
            }

            public static Builder<ServiceLoader<?>> builder() {
                return new Builder<ServiceLoader<?>>(ServiceLoader::load)
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static Builder<ServiceLoader<?>> builder(ClassLoader classLoader) {
                return new Builder<ServiceLoader<?>>(service -> ServiceLoader.load(service, classLoader))
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static <BACKEND> Builder<BACKEND> builder(Function<Class<?>, BACKEND> factory) {
                return new Builder<>(factory);
            }

            private SwingColorSchemeLoader(Iterable<Example.SwingColorScheme> source, Runnable sourceReloader,
                                           Iterable<Example.SwingColorSchemeBatch> batch, Runnable batchReloader,
                                           Function<Example.SwingColorSchemeBatch, Iterable<Example.SwingColorScheme>> mapper,
                                           Consumer<Throwable> onUnexpectedError) {
                super(source, sourceReloader, batch, batchReloader, mapper, onUnexpectedError);
            }

            public static final class Builder<BACKEND> extends BuilderTemplate<BACKEND, Builder<BACKEND>> {

                private Builder(Function<Class<?>, BACKEND> factory) {
                    super(factory);
                }

                public SwingColorSchemeLoader build() {
                    BACKEND sourceBackend = factory.apply(Example.SwingColorScheme.class);
                    BACKEND batchBackend = factory.apply(Example.SwingColorSchemeBatch.class);
                    return new SwingColorSchemeLoader(
                            asLoader(Example.SwingColorScheme.class, sourceBackend),
                            asReloader(sourceBackend),
                            asLoader(Example.SwingColorSchemeBatch.class, batchBackend),
                            asReloader(batchBackend),
                            asMapper(Example.SwingColorSchemeBatch::getProviders),
                            onUnexpectedError
                    );
                }
            }
        }

        static final class FileTypeSpiLoader extends MultipleTemplate<Example.FileTypeSpi, Void> {

            public static List<Example.FileTypeSpi> load() {
                return builder().build().get();
            }

            public static Builder<ServiceLoader<?>> builder() {
                return new Builder<ServiceLoader<?>>(ServiceLoader::load)
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            public static Builder<ServiceLoader<?>> builder(ClassLoader classLoader) {
                return new Builder<ServiceLoader<?>>(service -> ServiceLoader.load(service, classLoader))
                        .streamer(backend -> backend)
                        .reloader(ServiceLoader::reload);
            }

            private FileTypeSpiLoader(Iterable<Example.FileTypeSpi> source, Runnable sourceReloader,
                                      Iterable<Void> batch, Runnable batchReloader,
                                      Function<Void, Iterable<Example.FileTypeSpi>> mapper,
                                      Consumer<Throwable> onUnexpectedError) {
                super(source, sourceReloader, batch, batchReloader, mapper, onUnexpectedError);
            }

            public static final class Builder<BACKEND> extends BuilderTemplate<BACKEND, Builder<BACKEND>> {

                private Builder(Function<Class<?>, BACKEND> factory) {
                    super(factory);
                }

                public FileTypeSpiLoader build() {
                    BACKEND sourceBackend = factory.apply(Example.FileTypeSpi.class);
                    return new FileTypeSpiLoader(
                            asLoader(Example.FileTypeSpi.class, sourceBackend),
                            asReloader(sourceBackend),
                            noLoad(),
                            noReload(),
                            noMapping(),
                            onUnexpectedError
                    );
                }
            }
        }

        static final class InternalFileTypeSpiLoader {

            private final FileTypeSpiLoader delegate = FileTypeSpiLoader.builder().build();

            private final AtomicReference<List<Example.FileTypeSpi>> resource = new AtomicReference<>(delegate.get());

            public List<Example.FileTypeSpi> get() {
                return resource.get();
            }

            public void set(List<Example.FileTypeSpi> newValue) {
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
            catchUnexpectedErrors();
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
                    .streamer(Lookup.Result::allInstances)
                    .build()
                    .get().ifPresent(Demo::printWindowsVersion);

            printProperties(
                    ConfigurationLoader
                            .builder(Lookup.getDefault()::lookupResult)
                            .streamer(Lookup.Result::allInstances)
                            .build()
                            .get()
            );

            SwingColorSchemeLoader
                    .builder(Lookup.getDefault()::lookupResult)
                    .streamer(Lookup.Result::allInstances)
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
            Generated.InternalFileTypeSpiLoader adapter = new Generated.InternalFileTypeSpiLoader();
            adapter.set(emptyList());
            adapter.reload();
            adapter.reset();
            adapter.get();
        }

        private static void catchUnexpectedErrors() {
            SwingColorSchemeLoader
                    .builder()
                    .onUnexpectedError(ex -> Logger.getAnonymousLogger().log(Level.WARNING, "While loading", ex))
                    .build()
                    .get()
                    .forEach(Demo::printColors);
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
