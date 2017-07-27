/*
 * Double.java
 *
 * Created on March 8, 2007, 5:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

/**
 *
 * @author maurizio
 */
public class Double extends Term<Double> {
	final java.lang.Double _theDouble;

	@Override
    public <Z> Z/*java.lang.Double*/ toJava() {
		//return (Z)_theDouble;
		return uncheckedCast(_theDouble);
	}
	
	public Double (java.lang.Double d) {_theDouble = d;}
           
        @Override
        public alice.tuprolog.Double marshal() {
            return new alice.tuprolog.Double(_theDouble);
        }
        
        static Double unmarshal(alice.tuprolog.Double d) {
            if (!matches(d))
                throw new UnsupportedOperationException();
            return new Double(d.doubleValue());
        }
        
        static boolean matches(alice.tuprolog.Term t) {
            return (t instanceof alice.tuprolog.Double);
        }
        
	public String toString() {
		return "Double("+_theDouble+ ')';
	}

}