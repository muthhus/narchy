package alice.tuprologx.pj.model;

import java.util.*;

/**
 *
 * @author maurizio
 */
public class Compound2<X1 extends Term<?>,X2 extends Term<?>> extends Cons<X1,Cons<X2,Nil>> {    
    public Compound2(String name, X1 x1, X2 x2) {
        super(name,new Vector<Term<?>>(Arrays.asList(new Term<?>[]{x1,x2})));
    }
    
    public X1 get0() {return getHead();}
    
    public X2 get1() {return getRest().getHead();}
}
