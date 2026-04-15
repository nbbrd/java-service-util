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

import com.github.mustachejava.DefaultMustacheFactory;
import com.squareup.javapoet.ClassName;
import lombok.NonNull;
import nbbrd.service.Quantifier;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

/**
 * @author Philippe Charles
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@lombok.Value
@lombok.Builder
class LoadDefinition {

    @lombok.NonNull
    Quantifier quantifier;

    @lombok.NonNull
    ClassName serviceType;

    @lombok.NonNull
    Optional<TypeInstantiator> fallback;

    @lombok.NonNull
    String loaderName;

    @lombok.NonNull
    Optional<BatchDefinition> batch;

    public @NonNull ClassName resolveLoaderName() {
        return resolveName(loaderName, serviceType, "Loader");
    }

    // visible for testing
    static ClassName resolveName(String classNameString, ClassName serviceType, String defaultSuffix) {
        return NO_NAME.equals(classNameString)
                ? generateName(serviceType, defaultSuffix)
                : parseName(classNameString, serviceType);
    }

    private static ClassName parseName(String classNameString, ClassName serviceType) {
        StringWriter writer = new StringWriter();
        new DefaultMustacheFactory()
                .compile(new StringReader(classNameString), "")
                .execute(writer, MustacheContext.of(serviceType));
        ClassName parsed = ClassName.bestGuess(writer.toString());

        // For nested services using top-level class name templates, ensure the loader name is also nested
        ClassName top = serviceType.topLevelClassName();
        if (!top.equals(serviceType) && usesTopLevelTemplate(classNameString)) {
            // Service is nested and template uses top-level class name - make the loader name nested too
            return parsed.topLevelClassName().nestedClass(serviceType.simpleName());
        }
        return parsed;
    }

    private static boolean usesTopLevelTemplate(String classNameString) {
        return classNameString.contains("{{topLevelClassName}}")
                || classNameString.contains("{{topLevelSimpleName}}");
    }

    private static ClassName generateName(ClassName serviceType, String defaultSuffix) {
        ClassName top = serviceType.topLevelClassName();
        ClassName topLoader = ClassName.get(top.packageName(), top.simpleName() + defaultSuffix);
        if (top.equals(serviceType)) {
            return topLoader;
        }
        return topLoader.nestedClass(serviceType.simpleName());
    }

    public static final String NO_NAME = "";
}
