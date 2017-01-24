package nars.truth.func.annotation;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * marker annotation for truthfunctions which allow evidence overlap
 * (in double premise case) and cycle (in single premise case)
 */
@Retention(RUNTIME)
public @interface AllowOverlap {

}
