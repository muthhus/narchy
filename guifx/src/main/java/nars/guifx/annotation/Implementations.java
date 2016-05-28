package nars.guifx.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.FIELD)
public @interface Implementations {
    Implementation[] value() default{};
}
