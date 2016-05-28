package nars.guifx.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a choice of implementation classes
 */
@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.FIELD)
@Repeatable(value = Implementations.class)
public @interface Implementation {
    Class value();
}
