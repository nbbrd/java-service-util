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
 * <p>
 * Internal storage summary:
 * <pre>
 * optional none                  :        final           Optional&lt;T&gt;
 * optional basic                 :                        Optional&lt;T&gt;
 * optional concurrent            :        final AtomicRef&lt;Optional&lt;T&gt;&gt;
 * optional none       +singleton : static final           Optional&lt;T&gt;
 * optional basic      +singleton : static                 Optional&lt;T&gt;
 * optional concurrent +singleton : static final AtomicRef&lt;Optional&lt;T&gt;&gt;
 *
 * single   none                  :        final           T
 * single   basic                 :                        T
 * single   concurrent            :        final AtomicRef&lt;T&gt;
 * single   none       +singleton : static final           T
 * single   basic      +singleton : static                 T
 * single   concurrent +singleton : static final AtomicRef&lt;T&gt;
 *
 * multiple none                  :        final           UmodifiableList&lt;T&gt;
 * multiple basic                 :                        UmodifiableList&lt;T&gt;
 * multiple concurrent            :        final AtomicRef&lt;UmodifiableList&lt;T&gt;&gt;
 * multiple none       +singleton : static final           UmodifiableList&lt;T&gt;
 * multiple basic      +singleton : static                 UmodifiableList&lt;T&gt;
 * multiple concurrent +singleton : static final AtomicRef&lt;UmodifiableList&lt;T&gt;&gt;
 * </pre>
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
     * Specifies the mutability of the loader.
     *
     * @return a non-null mutability
     */
    Mutability mutability() default Mutability.NONE;

    /**
     * Specifies if the loader must be a singleton.
     *
     * @return true if the loader is a singleton, false otherwise
     */
    boolean singleton() default false;

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
     * Specifies if fallback class is unexpected.
     *
     * @return true if fallback class is expected, false otherwise
     */
    boolean noFallback() default false;

    /**
     * Specifies the wrapper class to be used in basic preprocessing.
     * <p>
     * Requirements:
     * <ul>
     * <li>must be assignable to the service type
     * <li>must be instantiable either by constructor or static method, both
     * with single parameter of service type
     * </ul>
     *
     * @return the wrapper class if required, {@link Void} otherwise
     */
    Class<?> wrapper() default Void.class;

    /**
     * Specifies the preprocessor class to be used in advanced
     * preprocessing.<br>This operation happens between loading and
     * storage.<br>It may include filtering, sorting and mapping.
     * <p>
     * Requirements:
     * <ul>
     * <li>must be assignable to {@code UnaryOperator<? extends Stream<SERVICE_TYPE>>}
     * <li>must be instantiable either by constructor, static method, enum field
     * or static final field
     * </ul>
     *
     * @return the preprocessor class if required, {@link NoProcessing}
     * otherwise
     */
    Class<? extends UnaryOperator<? extends Stream>> preprocessor() default NoProcessing.class;

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
     */
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
     */
    Class<? extends Consumer<? extends Iterable>> cleaner() default DefaultCleaner.class;

    /**
     * Specifies if batch loading should be allowed.
     *
     * @return true if batch loading should be allowed, false otherwise
     */
    boolean batch() default false;

    /**
     * Specifies the fully qualified name of the batch loading.
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
    String batchName() default "";

    @SuppressWarnings("rawtypes")
    final class NoProcessing implements UnaryOperator<Stream> {

        @Override
        public Stream apply(Stream t) {
            return t;
        }
    }

    @SuppressWarnings("rawtypes")
    final class DefaultBackend implements Function<Class, Iterable> {

        @SuppressWarnings("unchecked")
        @Override
        public Iterable apply(Class type) {
            return ServiceLoader.load(type);
        }
    }

    @SuppressWarnings("rawtypes")
    final class DefaultCleaner implements Consumer<Iterable> {

        @Override
        public void accept(Iterable serviceLoader) {
            ((ServiceLoader) serviceLoader).reload();
        }
    }
}
