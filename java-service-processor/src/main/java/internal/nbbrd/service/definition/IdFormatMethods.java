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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of built-in JDK types and their no-arg String-returning format methods
 * for use with {@code @ServiceId} without an explicit {@code formatMethodName}.
 * <p>
 * Resolution priority (first match wins):
 * <ol>
 *   <li>Explicit {@code @ServiceId(formatMethodName=...)} value</li>
 *   <li>{@code @nbbrd.design.RepresentableAsString(formatMethodName=...)} on the return type</li>
 *   <li>Built-in JDK registry ({@link #BUILT_IN})</li>
 * </ol>
 *
 * @author Philippe Charles
 */
final class IdFormatMethods {

    private static final String REPRESENTABLE_AS_STRING = "nbbrd.design.RepresentableAsString";
    private static final String FORMAT_METHOD_NAME_ATTR = "formatMethodName";

    /**
     * Ordered map of well-known JDK type canonical names to the name of their
     * canonical no-arg, String-returning method. Checked via {@code isAssignable}
     * so subclasses (e.g. every enum constant) are automatically covered.
     */
    static final Map<String, String> BUILT_IN;

    static {
        BUILT_IN = new LinkedHashMap<>();
        BUILT_IN.put("java.lang.Enum", "name");           // covers all enum types
        BUILT_IN.put("java.util.UUID", "toString");
        BUILT_IN.put("java.net.URI", "toString");
        BUILT_IN.put("java.nio.charset.Charset", "name");
        BUILT_IN.put("java.util.Locale", "toLanguageTag");
    }

    private IdFormatMethods() {
    }

    /**
     * Resolves the built-in format method name for the given return type, if any.
     *
     * @param returnType the return type of the {@code @ServiceId}-annotated method
     * @param types      type utilities
     * @param elements   element utilities
     * @return the built-in format method name, or empty if the type is not in the registry
     */
    static Optional<String> resolve(TypeMirror returnType, Types types, Elements elements) {
        TypeMirror erasedReturnType = types.erasure(returnType);
        for (Map.Entry<String, String> entry : BUILT_IN.entrySet()) {
            TypeElement builtInElement = elements.getTypeElement(entry.getKey());
            if (builtInElement != null && types.isAssignable(erasedReturnType, types.erasure(builtInElement.asType()))) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Reads the {@code formatMethodName} attribute from a
     * {@code @nbbrd.design.RepresentableAsString} annotation on the return type element,
     * if present. Uses annotation mirrors so that the annotation class does not need
     * to be on the processor classpath.
     *
     * @param returnType the return type of the {@code @ServiceId}-annotated method
     * @param types      type utilities
     * @return the format method name declared by {@code @RepresentableAsString}, or empty
     */
    static Optional<String> resolveFromRepresentableAsString(TypeMirror returnType, Types types) {
        TypeElement returnTypeElement = (TypeElement) types.asElement(returnType);
        if (returnTypeElement == null) return Optional.empty();

        for (AnnotationMirror mirror : returnTypeElement.getAnnotationMirrors()) {
            if (!mirror.getAnnotationType().toString().equals(REPRESENTABLE_AS_STRING)) continue;

            // Check explicitly set value first
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals(FORMAT_METHOD_NAME_ATTR)) {
                    String value = entry.getValue().getValue().toString();
                    if (!value.isEmpty()) return Optional.of(value);
                }
            }

            // Fall back to the annotation attribute's declared default value
            for (ExecutableElement attr : ElementFilter.methodsIn(mirror.getAnnotationType().asElement().getEnclosedElements())) {
                if (attr.getSimpleName().contentEquals(FORMAT_METHOD_NAME_ATTR) && attr.getDefaultValue() != null) {
                    String defaultValue = attr.getDefaultValue().getValue().toString();
                    if (!defaultValue.isEmpty()) return Optional.of(defaultValue);
                }
            }

            return Optional.empty(); // annotation found but no usable formatMethodName
        }
        return Optional.empty();
    }
}

