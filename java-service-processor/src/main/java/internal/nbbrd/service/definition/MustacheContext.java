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
package internal.nbbrd.service.definition;

import com.squareup.javapoet.ClassName;
import lombok.NonNull;

/**
 * Mustache template context for loader name resolution.
 * Provides string properties that can be accessed via {{propertyName}} in Mustache templates.
 *
 * @author Philippe Charles
 */
@lombok.Value
class MustacheContext {

    /**
     * The package name of the service class, or "" if in the default package.
     */
    @NonNull
    String packageName;

    /**
     * The simple name of the service class (without package or enclosing class).
     */
    @NonNull
    String simpleName;

    /**
     * The full canonical name of the service class.
     */
    @NonNull
    String canonicalName;

    /**
     * The simple name of the top-level class (for nested services, this is the enclosing class name).
     */
    @NonNull
    String topLevelClassName;

    static MustacheContext of(ClassName serviceType) {
        return new MustacheContext(
                serviceType.packageName(),
                serviceType.simpleName(),
                serviceType.canonicalName(),
                serviceType.topLevelClassName().simpleName()
        );
    }
}

