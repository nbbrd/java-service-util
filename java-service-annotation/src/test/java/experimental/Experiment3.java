package experimental;

import java.lang.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Experiment3 {

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceProviderInterface {

        @Documented
        @Target({ElementType.TYPE})
        @Retention(RetentionPolicy.SOURCE)
        @interface Optional {

            /**
             * Specifies the name of the loader. An empty value
             * generates an automatic name.
             *
             * @return a class name
             */
            ClassName loader() default @ClassName;
        }

        @Documented
        @Target({ElementType.TYPE})
        @Retention(RetentionPolicy.SOURCE)
        @interface Single {

            /**
             * Specifies the name of the loader. An empty value
             * generates an automatic name.
             *
             * @return a class name
             */
            ClassName loader() default @ClassName;

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
        @interface Multiple {

            /**
             * Specifies the name of the loader. An empty value
             * generates an automatic name.
             *
             * @return a class name
             */
            ClassName loader() default @ClassName;

            /**
             * Specifies the name of the batch loading. An empty value
             * generates an automatic name.
             *
             * @return a class name
             */
            ClassName batch() default @ClassName;

            /**
             * Specifies if batch loading is disabled.
             *
             * @return true if batch loading is disabled, false otherwise
             */
            boolean noBatch() default false;
        }
    }

    static class Demo {

        @ServiceProviderInterface.Optional
        interface OptionalService {
        }

        @ServiceProviderInterface.Optional(loader = @ClassName(simpleName = "Other", packageName = "internal"))
        interface OptionalServiceWithCustomLoader {
        }

        @ServiceProviderInterface.Single(fallback = SingleServiceFallback.class)
        interface SingleService {
        }

        static class SingleServiceFallback implements SingleService {
        }

        @ServiceProviderInterface.Multiple
        interface MultiService {
        }

        @ServiceProviderInterface.Multiple(batch = @ClassName(simpleName = "Stuff"))
        interface MultiServiceWithCustomBatch {
        }

        @ServiceProviderInterface.Multiple(noBatch = true)
        interface MultiServiceWithoutBatch {
        }

        public static void main(String[] args) {
            MultiServiceLoader loader = MultiServiceLoader.ofServiceLoader();
            loader.get().forEach(System.out::println);
            loader.reload();
            loader.get().forEach(System.out::println);
        }

        interface MultiServiceBatch {
            Stream<MultiService> getProviders();
        }

        static final class MultiServiceLoader {

            public static MultiServiceLoader ofServiceLoader() {
                return of(ServiceLoader::load, ServiceLoader::reload);
            }

            public static MultiServiceLoader ofServiceLoader(ClassLoader classLoader) {
                return of(service -> ServiceLoader.load(service, classLoader), ServiceLoader::reload);
            }

            public static <BACKEND extends Iterable> MultiServiceLoader of(Function<Class<?>, BACKEND> backend, Consumer<BACKEND> reloader) {
                return new MultiServiceLoader(backend.apply(MultiService.class), backend.apply(MultiServiceBatch.class), o -> reloader.accept((BACKEND) o));
            }

            private final Iterable<MultiService> source;
            private final Iterable<MultiServiceBatch> batch;
            private final Consumer<Iterable<?>> reloader;

            private List<MultiService> resource;

            private MultiServiceLoader(Iterable<MultiService> source, Iterable<MultiServiceBatch> batch, Consumer<Iterable<?>> reloader) {
                this.source = source;
                this.batch = batch;
                this.reloader = reloader;
                this.resource = null; // lazy
            }

            private List<MultiService> doLoad() {
                return Stream.concat(
                                StreamSupport.stream(source.spliterator(), false),
                                StreamSupport.stream(batch.spliterator(), false).flatMap(MultiServiceBatch::getProviders))
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
            }

            public List<MultiService> get() {
                if (resource == null) {
                    resource = doLoad();
                }
                return resource;
            }

            public void reload() {
                reloader.accept(source);
                reloader.accept(batch);
                resource = null;
            }
        }
    }
}
