/*
 * PrologMethod.java
 *
 * Created on March 8, 2007, 5:28 PM
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
@Target(ElementType.METHOD)
public @interface PrologMethod {
    String[] clauses() default {};    
    String predicate() default "";
    String signature() default "";
    String[] types() default {};
    boolean exceptionOnFailure() default false;    
    boolean keepSubstitutions() default false;
}

