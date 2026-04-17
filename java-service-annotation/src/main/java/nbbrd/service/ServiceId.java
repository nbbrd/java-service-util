package nbbrd.service;

import java.lang.annotation.*;

/**
 * Specifies that a method can be used to retrieve an identifier on a service.
 * This annotation must be used in conjunction with {@link ServiceDefinition}.
 * <p>
 * The annotated method must follow the following rules:
 * <ol>
 *     <li>Id method only applies to methods of a service</li>
 *     <li>Id method does not apply to static methods</li>
 *     <li>Id method must have no-args</li>
 *     <li>Id method must return {@link String}, a built-in representable type, or any type
 *         with a format method specified via {@code formatMethodName}</li>
 *     <li>Id method must be unique</li>
 *     <li>Id method must not throw checked exceptions</li>
 *     <li>Id pattern must be valid</li>
 *     <li>Format method (if specified) must exist on the return type, be no-arg and return {@link String}</li>
 * </ol>
 * <p>
 * The following JDK types are recognized as built-in representable types and do not require
 * an explicit {@code formatMethodName}:
 * <ul>
 *     <li>All {@link Enum} subtypes — uses {@code name()}</li>
 *     <li>{@code java.util.UUID} — uses {@code toString()}</li>
 *     <li>{@code java.net.URI} — uses {@code toString()}</li>
 *     <li>{@code java.nio.charset.Charset} — uses {@code name()}</li>
 *     <li>{@code java.util.Locale} — uses {@code toLanguageTag()}</li>
 * </ul>
 * <p>
 * Additionally, if the return type is annotated with {@code @nbbrd.design.RepresentableAsString},
 * its {@code formatMethodName} attribute is used automatically (without requiring that annotation
 * on the processor classpath).
 * <p>
 * Resolution priority (first match wins):
 * <ol>
 *     <li>Explicit {@code @ServiceId(formatMethodName=...)} value</li>
 *     <li>{@code @RepresentableAsString(formatMethodName=...)} on the return type</li>
 *     <li>Built-in JDK registry listed above</li>
 *     <li>Direct {@link String} return type</li>
 * </ol>
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface ServiceId {

    /**
     * Specifies the regex pattern that the ID is expected to match.<br>
     * If specified, this pattern will be added to the loader as a static final field.
     *
     * @return a regex pattern if required, empty otherwise
     */
    String pattern() default "";

    /**
     * Specifies the name of a no-arg method on the return type of the annotated method
     * that converts it to a unique {@link String} representation.<br>
     * When specified, the annotated method may return any type (not just {@link String}),
     * as long as that type exposes the named method returning {@link String}.
     * <p>
     * This attribute is optional for types listed as built-in representable types
     * (see class-level documentation). It is only required for types not in that list.
     * <p>
     * Example: {@code @ServiceId(formatMethodName = "name")} on {@code MyEnum getCategory()}
     * will use {@code provider.getCategory().name()} as the identifier.
     * However, any {@link Enum} subtype already uses {@code name()} automatically,
     * so the explicit {@code formatMethodName} is not needed in that case.
     *
     * @return the name of the format method, or empty to use the default for built-in types
     *         or require a direct {@link String} return type
     */
    String formatMethodName() default "";

    String FLAT_CASE = "^[a-z0-9]+$";
    String UPPER_FLAT_CASE = "^[A-Z0-9]+$";
    String CAMEL_CASE = "^[a-z]+(?:[A-Z0-9]+[a-z0-9]+[A-Za-z0-9]*)*$";
    String PASCAL_CASE = "^(?:[A-Z][a-z0-9]+)(?:[A-Z]+[a-z0-9]*)*$";
    String SNAKE_CASE = "^[a-z0-9]+(?:_[a-z0-9]+)*$";
    String SCREAMING_SNAKE_CASE = "^[A-Z0-9]+(?:_[A-Z0-9]+)*$";
    String CAMEL_SNAKE_CASE = "^[A-Z][a-z0-9]+(?:_[A-Z]+[a-z0-9]*)*$";
    String KEBAB_CASE = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
    String SCREAMING_KEBAB_CASE = "^[A-Z0-9]+(?:-[A-Z0-9]+)*$";
    String TRAIN_CASE = "^[A-Z][a-z0-9]+(?:-[A-Z]+[a-z0-9]*)*$";
}
