package io.github.chris013.practice.task30;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the JSON member name of a record component. Without it, the component name is used.
 *
 * <p>RUNTIME retention is required: the mapper reads it through reflection while the program runs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface JsonName {
    String value();
}
