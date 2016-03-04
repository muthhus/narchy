/*
 * JavaObject.java
 *
 * Created on July 19, 2007, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import alice.tuprolog.PTerm;
import alice.tuprologx.pj.engine.PJ;

/**
 *
 * @author maurizio
 */
public class JavaObject<O> extends Term<JavaObject<O>> {
    
    O _theObject;
    
    /** Creates a new instance of JavaObject */
    public JavaObject(O o) {
        _theObject = o;
    }

    @Override
    public PTerm marshal() {
        return PJ.registerJavaObject(_theObject);
    }

    @Override
    public <Z> Z toJava() {
        //return (Z)_theObject;
        return uncheckedCast(_theObject);
    }
    
    static boolean matches(PTerm t) {
        return (t instanceof alice.tuprolog.Struct && PJ.getRegisteredJavaObject((alice.tuprolog.Struct)t) != null);        
    }
    
    static <Z> JavaObject<Z> unmarshalObject(alice.tuprolog.Struct s) {
        if (matches(s)) {           
            //return new JavaObject<Z>((Z)(PJ.getRegisteredJavaObject(s)));
        	Z auxJavaObject = uncheckedCast(PJ.getRegisteredJavaObject(s));
            return new JavaObject<Z>( auxJavaObject );
        }
        else
            throw new UnsupportedOperationException();
    }

    public String toString() {        
        return "JavaObject{"+_theObject+"}";
    }
    
    
    
}
