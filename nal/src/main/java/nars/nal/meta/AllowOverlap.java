package nars.nal.meta;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * marker annotation for truthfunctions which allow evidence overlap
 */
@Retention(value=RUNTIME)
public @interface AllowOverlap {

}
