package experimental;

import java.awt.*;
import java.lang.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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

    static class Demo {

        @OptionalService
        interface WinRegistry {
            String readString(int hkey, String key, String valueName);

            int HKEY_LOCAL_MACHINE = 0;
        }

        @SingleService(fallback = SystemProperties.class)
        interface Configuration {
            Properties getProperties();
        }

        static class SystemProperties implements Configuration {

            @Override
            public Properties getProperties() {
                return System.getProperties();
            }
        }

        @MultipleService
        interface SwingColorScheme {
            List<Color> getColors();
        }

        public static void main(String[] args) {
            Optional<WinRegistry> winRegistry = Generated.WinRegistryLoader.ofServiceLoader().get();
            winRegistry.ifPresent(reg -> System.out.println(reg.readString(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName")));

            Configuration configuration = Generated.ConfigurationLoader.ofServiceLoader().get();
            configuration.getProperties().forEach((k, v) -> System.out.println(k + "=" + v));

            List<SwingColorScheme> swingColorSchemes = Generated.SwingColorSchemeLoader.ofServiceLoader().get();
            swingColorSchemes.forEach(colorScheme -> System.out.println(colorScheme.getColors()));
        }
    }

    static class Generated {

        static final class WinRegistryLoader {

            public static WinRegistryLoader ofServiceLoader() {
                return ofServiceLoader(Thread.currentThread().getContextClassLoader());
            }

            public static WinRegistryLoader ofServiceLoader(ClassLoader classLoader) {
                return of(service -> ServiceLoader.load(service, classLoader), ServiceLoader::reload);
            }

            public static <BACKEND extends Iterable<?>> WinRegistryLoader of(Function<Class<?>, BACKEND> backend, Consumer<BACKEND> reloader) {
                throw new UnsupportedOperationException();
            }

            public Optional<Demo.WinRegistry> get() {
                throw new UnsupportedOperationException();
            }

            public void reload() {
            }
        }

        static final class ConfigurationLoader {

            public static ConfigurationLoader ofServiceLoader() {
                return ofServiceLoader(Thread.currentThread().getContextClassLoader());
            }

            public static ConfigurationLoader ofServiceLoader(ClassLoader classLoader) {
                return of(service -> ServiceLoader.load(service, classLoader), ServiceLoader::reload);
            }

            public static <BACKEND extends Iterable<?>> ConfigurationLoader of(Function<Class<?>, BACKEND> backend, Consumer<BACKEND> reloader) {
                throw new UnsupportedOperationException();
            }

            public Demo.Configuration get() {
                throw new UnsupportedOperationException();
            }

            public void reload() {
            }
        }

        interface SwingColorSchemeBatch {
            Stream<Demo.SwingColorScheme> getProviders();
        }

        static final class SwingColorSchemeLoader {

            public static SwingColorSchemeLoader ofServiceLoader() {
                return ofServiceLoader(Thread.currentThread().getContextClassLoader());
            }

            public static SwingColorSchemeLoader ofServiceLoader(ClassLoader classLoader) {
                return of(service -> ServiceLoader.load(service, classLoader), ServiceLoader::reload);
            }

            public static <BACKEND extends Iterable<?>> SwingColorSchemeLoader of(Function<Class<?>, BACKEND> backend, Consumer<BACKEND> reloader) {
                throw new UnsupportedOperationException();
            }

            public List<Demo.SwingColorScheme> get() {
                throw new UnsupportedOperationException();
            }

            public void reload() {
            }
        }
    }
}
