/*
 * InvalidPredicateException.java
 *
 * Created on May 4, 2007, 12:42 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.meta;

/**
 *
 * @author maurizio
 */
@SuppressWarnings("serial")
public class InvalidPredicateException extends RuntimeException {
    
    /** Creates a new instance of InvalidPredicateException */
    public InvalidPredicateException(String s) {
        super(s);
    }
    
}
