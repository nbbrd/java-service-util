/*
 * Copyright 2019 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package nbbrd.service;

import java.lang.annotation.*;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Declarative definition of a service that generates a specialized service
 * loader that takes care of the loading and enforces a specific usage.
 *
 * @author Philippe Charles
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceDefinition {

    /**
     * Specifies how many instances are returned by the loader.
     *
     * @return a non-null quantifier
     */
    Quantifier quantifier() default Quantifier.OPTIONAL;

    /**
     * Specifies the fully qualified name of the loader.<br>
     * An empty value generates an automatic name.
     * A non-empty value is interpreted as a <a href="https://mustache.github.io/">Mustache template</a> with the following tags:
     * <ul>
     *     <li><code>{{packageName}}</code>: The package name of the service class, or "" if this is in the default package.</li>
     *     <li><code>{{simpleName}}</code>: The service class name.</li>
     *     <li><code>{{canonicalName}}</code>: The full service class name.</li>
     * </ul>
     *
     * @return a fully qualified name
     */
    String loaderName() default "";

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
    Class<?> fallback() default Void.class;

    /**
     * Specifies the batch class to use in batch loading.
     * <p>
     * Requirements:
     * <ol>
     * <li>Batch type must be an interface or an abstract class</li>
     * <li>Batch method must be unique</li>
     * </ol>
     *
     * @return the batch class if required, {@link Void} otherwise
     */
    Class<?> batchType() default Void.class;

    /**
     * Specifies the mutability of the loader.
     *
     * @return a non-null mutability
     * @deprecated This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release.
     */
    @Deprecated
    Mutability mutability() default Mutability.NONE;

    /**
     * Specifies the class that creates a service loader.
     * <br>The default backend uses {@link ServiceLoader#load(Class)}.
     * <p>
     * Requirements:
     * <ul>
     * <li>must be assignable to {@code Function<Class, ? extends Iterable>}
     * <li>must be instantiable either by constructor, static method, enum field
     * or static final field
     * </ul>
     *
     * @return the backend class if required, {@link DefaultBackend}
     * otherwise
     * @deprecated This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release.
     */
    @Deprecated
    Class<? extends Function<? extends Class, ? extends Iterable>> backend() default DefaultBackend.class;

    /**
     * Specifies the class that deals with the cache cleaning.
     * <br>The default cleaner uses {@link ServiceLoader#reload()}.
     * <p>
     * Requirements:
     * <ul>
     * <li>must be assignable to {@code Consumer<? extends Iterable>}
     * <li>must be instantiable either by constructor, static method, enum field
     * or static final field
     * </ul>
     *
     * @return the backend class if required, {@link DefaultCleaner}
     * otherwise
     * @deprecated This is a complex mechanism that targets specific usages. It will be removed and/or simplified in a future release.
     */
    @Deprecated
    Class<? extends Consumer<? extends Iterable>> cleaner() default DefaultCleaner.class;

    @SuppressWarnings("rawtypes")
    @Deprecated
    final class DefaultBackend implements Function<Class, Iterable> {

        @SuppressWarnings("unchecked")
        @Override
        public Iterable apply(Class type) {
            return ServiceLoader.load(type);
        }
    }

    @SuppressWarnings("rawtypes")
    @Deprecated
    final class DefaultCleaner implements Consumer<Iterable> {

        @Override
        public void accept(Iterable serviceLoader) {
            ((ServiceLoader) serviceLoader).reload();
        }
    }

    /**
     * Name to suppress single-fallback warning using @{@link SuppressWarnings}
     */
    String SINGLE_FALLBACK_NOT_EXPECTED = "SingleFallbackNotExpected";
}
