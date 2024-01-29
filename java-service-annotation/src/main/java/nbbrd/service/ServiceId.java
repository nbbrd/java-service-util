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
 *     <li>Id method must return String</li>
 *     <li>Id pattern must be valid</li>
 * </ol>
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceId {

    /**
     * Specifies the regex pattern that the ID is expected to match.<br>
     * If specified, this pattern will be added to the loader as a static final field.
     *
     * @return a regex pattern if required, empty otherwise
     */
    String pattern() default "";

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
