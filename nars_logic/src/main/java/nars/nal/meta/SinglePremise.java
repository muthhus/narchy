package nars.nal.meta;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * marker annotation for single-premise truthfunctions
 */
@Retention(value=RUNTIME)
public @interface SinglePremise {

}
