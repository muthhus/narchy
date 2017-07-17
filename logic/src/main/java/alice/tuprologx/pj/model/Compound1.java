package alice.tuprologx.pj.model;

import java.util.Arrays;
import java.util.Vector;

/**
 *
 * @author maurizio
 */
public class Compound1<X1 extends Term<?>> extends Cons<X1,Nil> {    
    public Compound1(String name, X1 x1) {
        super(name, new Vector<>(Arrays.asList(x1)));
    }
    
    public X1 get0() {return getHead();}
}


