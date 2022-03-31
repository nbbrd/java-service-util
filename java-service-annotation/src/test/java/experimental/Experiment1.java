package experimental;

import nbbrd.service.Quantifier;

import java.lang.annotation.*;

public class Experiment1 {

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServiceProviderInterface {

        /**
         * Specifies how many instances are returned by the loader.
         *
         * @return a non-null quantifier
         */
        Quantifier quantifier() default Quantifier.OPTIONAL;

        /**
         * Specifies the fallback class to use if no service is available.<br>This
         * option is only used in conjunction with {@link Quantifier#SINGLE}.
         * <p>
         * Requirements:
         * <ul>
         * <li>must be assignable to the service type
         * <li>must be instantiable either by constructor, static method, enum field
         * or static final field
         * </ul>
         *
         * @return the fallback class if required, {@link Void} otherwise
         */
        Class<?> singleFallback() default Void.class;

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

    static class Demo {

        @ServiceProviderInterface
        interface OptionalService {
        }

        @ServiceProviderInterface(loader = @ClassName(simpleName = "Other", packageName = "internal"))
        interface OptionalServiceWithCustomLoader {
        }

        @ServiceProviderInterface(quantifier = Quantifier.SINGLE, singleFallback = SingleServiceFallback.class)
        interface SingleService {
        }

        static class SingleServiceFallback implements SingleService {
        }

        @ServiceProviderInterface(quantifier = Quantifier.MULTIPLE)
        interface MultiService {
        }

        @ServiceProviderInterface(quantifier = Quantifier.MULTIPLE, batch = @ClassName(simpleName = "Stuff"))
        interface MultiServiceWithCustomBatch {
        }

        @ServiceProviderInterface(quantifier = Quantifier.MULTIPLE, noBatch = true)
        interface MultiServiceWithoutBatch {
        }

        public static void main(String[] args) {
        }
    }
}
