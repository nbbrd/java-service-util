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
 * </ol>
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceId {
}
