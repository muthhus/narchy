/*
 * Atom.java
 *
 * Created on March 8, 2007, 5:10 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import alice.tuprolog.PTerm;

/**
 *
 * @author maurizio
 */
public class Atom extends Term<Atom> {
	String _theAtom;

	public Atom (String s) {_theAtom=s;}
	
	@Override
    public <Z> Z toJava() {
		//return (Z)_theAtom;
		return uncheckedCast(_theAtom);
	}

	public String toString() {
		return "Atom("+_theAtom+")";
	}
        
        @Override
        public alice.tuprolog.Struct marshal() {
            return new alice.tuprolog.Struct(_theAtom); 
        }
        
        static Atom unmarshal(alice.tuprolog.Struct a) {
            if (!matches(a))
                throw new UnsupportedOperationException();
            return new Atom(a.getName());            
        }
        
        static boolean matches(PTerm t) {
            return (!(t instanceof alice.tuprolog.Var) && t.isAtom() && !t.isList() && !Bool.matches(t));
        }
        
        public List<Atom> toCharList() {
            char[] carr = _theAtom.toCharArray();
            java.util.Vector<String> vs = new java.util.Vector<String>();
            for (char c : carr) {
                vs.add(c+"");
            }
            return new List<Atom>(vs);
        }
        
        public List<Atom> split(String regexp) {
            java.util.Vector<String> vs = new java.util.Vector<String>();
            for (String s : _theAtom.split(regexp)) {      
                vs.add(s);
            }      
            return new List<Atom>(vs);
        }
}