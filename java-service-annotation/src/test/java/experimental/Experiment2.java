package experimental;

import java.lang.annotation.*;

public class Experiment2 {

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OptionalServiceProviderInterface {

        /**
         * Specifies the name of the loader. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        String loader() default "";
    }

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SingleServiceProviderInterface {

        /**
         * Specifies the name of the loader. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        String loader() default "";

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
    public @interface MultipleServiceProviderInterface {

        /**
         * Specifies the name of the loader. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        String loader() default "";

        /**
         * Specifies the name of the batch loading. An empty value
         * generates an automatic name.
         *
         * @return a class name
         */
        String batch() default "";

        /**
         * Specifies if batch loading is disabled.
         *
         * @return true if batch loading is disabled, false otherwise
         */
        boolean noBatch() default false;
    }

    static class Demo {

        @OptionalServiceProviderInterface
        interface OptionalService {
        }

        @OptionalServiceProviderInterface(loader = "internal.Other")
        interface OptionalServiceWithCustomLoader {
        }

        @SingleServiceProviderInterface(fallback = SingleServiceFallback.class)
        interface SingleService {
        }

        static class SingleServiceFallback implements SingleService {
        }

        @MultipleServiceProviderInterface
        interface MultiService {
        }

        @MultipleServiceProviderInterface(batch = "{{packageName}}.Stuff")
        interface MultiServiceWithCustomBatch {
        }

        @MultipleServiceProviderInterface(noBatch = true)
        interface MultiServiceWithoutBatch {
        }

        public static void main(String[] args) {
        }
    }
}
