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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
     * @return
     */
    Quantifier quantifier() default Quantifier.OPTIONAL;

    /**
     * Specifies the mutability of the loader.
     *
     * @return
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
     * option is only used in conjunction with {@link Quantifier.SINGLE}.
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
     * Specifies the wrapper class to be used in basic preprocessing.
     * <p>
     * Requirements:
     * <ul>
     * <li>must be assignable to the service type
     * <li>must be instantiable by constructor with single parameter of service
     * type
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
     * <li>must be assignable to {@code UnaryOperator<? extends Stream<T>>}
     * <li>must be instantiable either by constructor, static method, enum field
     * or static final field
     * </ul>
     *
     * @return the preprocessor class if required, {@link NoProcessing}
     * otherwise
     */
    Class<? extends UnaryOperator<? extends Stream>> preprocessor() default NoProcessing.class;

    /**
     * Specifies the fully qualified name of the loader. An empty value
     * generates an automatic name.
     *
     * @return a fully qualified name
     */
    String loaderName() default "";

    static final class NoProcessing implements UnaryOperator<Stream> {

        @Override
        public Stream apply(Stream t) {
            return t;
        }
    }
}
