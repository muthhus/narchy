/*
 * UnexpectedEndOfExpressionException.java
 *
 * Created on April 26, 2007, 4:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.annotations.parser;

/**
 *
 * @author maurizio
 */
@SuppressWarnings("serial")
public class UnexpectedEndOfExpressionException extends RuntimeException {
    public UnexpectedEndOfExpressionException(Exception e) {super(e);}
}