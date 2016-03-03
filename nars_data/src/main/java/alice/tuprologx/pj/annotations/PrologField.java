/*
 * PrologField.java
 *
 * Created on 5 aprile 2007, 9.25
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.annotations;

import java.lang.annotation.*;

/**
 *
 * @author maurizio
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PrologField {
    String init() default("");
    String predicate() default("");
}