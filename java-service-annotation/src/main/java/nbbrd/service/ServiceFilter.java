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

/**
 * Specifies that a method must be used as a filter on a service.
 * This annotation must be used in conjunction with {@link ServiceDefinition}.
 * <p>
 * The annotated method must follow the following rules:
 * <ol>
 *     <li>Filter method only applies to methods of a service</li>
 *     <li>Filter method does not apply to static methods</li>
 *     <li>Filter method must have no-args</li>
 *     <li>Filter method must return boolean</li>
 *     <li>Filter method must not throw checked exceptions</li>
 * </ol>
 *
 * @author Philippe Charles
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceFilter {

    /**
     * Applies a logical negation.
     *
     * @return true if negation is required, false otherwise
     */
    boolean negate() default false;

    /**
     * Sets the filter ordering in case of multiple filters.
     *
     * @return an ordering
     */
    int position() default Integer.MAX_VALUE;
}
