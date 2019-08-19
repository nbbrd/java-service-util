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
 * Declarative definition of a service.
 *
 * @author Philippe Charles
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceDefinition {

    // Cardinality: optional, single or multi
    // Mutability: immutable, mutable, threadsafe
    // Modifiers: singleton
    //
    // optional immutable             :        final           Optional<T>
    // optional mutable               :                        Optional<T>
    // optional threadsafe            :        final AtomicRef<Optional<T>>
    // optional immutable  +singleton : static final           Optional<T>
    // optional mutable    +singleton : static                 Optional<T>
    // optional threadsafe +singleton : static final AtomicRef<Optional<T>>
    //
    // single   immutable             :        final           T
    // single   mutable               :                        T
    // single   threadsafe            :        final AtomicRef<T>
    // single   immutable  +singleton : static final           T
    // single   mutable    +singleton : static                 T
    // single   threadsafe +singleton : static final AtomicRef<T>
    //
    // multi    immutable             :        final           UmodifiableList<T>
    // multi    mutable               :                        UmodifiableList<T>
    // multi    threadsafe            :        final AtomicRef<UmodifiableList<T>>
    // multi    immutable  +singleton : static final           UmodifiableList<T>
    // multi    mutable    +singleton : static                 UmodifiableList<T>
    // multi    threadsafe +singleton : static final AtomicRef<UmodifiableList<T>>
    //
    //
    Quantifier quantifier() default Quantifier.OPTIONAL;

    Mutability mutability() default Mutability.NONE;

    boolean singleton() default false;

    Class<?> fallback() default NullValue.class;

    Class<? extends UnaryOperator<? extends Stream>> lookup() default NullValue.class;

    String loaderName() default "";

    static final class NullValue implements UnaryOperator<Stream> {

        @Override
        public Stream apply(Stream t) {
            throw new UnsupportedOperationException();
        }
    }
}
