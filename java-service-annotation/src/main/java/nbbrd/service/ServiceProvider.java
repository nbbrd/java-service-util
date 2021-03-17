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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative registration of a service provider.
 * <p>
 * Current features:
 * <ul>
 * <li>generates classpath files in {@code META-INF/services} folder
 * <li>supports multiple registration of one class
 * <li>can infer the service if the provider implements/extends exactly one
 * interface/class
 * <li>checks coherence between classpath and modulepath if
 * {@code module-info.java} is available
 * </ul>
 * <p>
 * Current limitations:
 * <ul>
 * <li>detects modulepath {@code public static provider()} method but doesn't
 * generate a workaround for classpath
 * </ul>
 *
 * @author Philippe Charles
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(ServiceProvider.List.class)
public @interface ServiceProvider {

    /**
     * The interface (or class) to register this implementation under.<br>This
     * value is optional if the provider implements/extends exactly one
     * interface/class.
     *
     * @return a non-null type
     */
    Class<?> value() default Void.class;

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.SOURCE)
    @interface List {

        ServiceProvider[] value();
    }
}
