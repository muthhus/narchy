package alice.tuprologx.pj.model;

import java.util.*;

/**
 *
 * @author maurizio
 */
public class Compound3<X1 extends Term<?>,X2 extends Term<?>,X3 extends Term<?>> extends Cons<X1,Cons<X2,Cons<X3,Nil>>> {    
    public Compound3(String name, X1 x1, X2 x2, X3 x3) {
        super(name,new Vector<Term<?>>(Arrays.asList(new Term<?>[]{x1,x2,x3})));
    }
    
    public X1 get0() {return getHead();}
    
    public X2 get1() {return getRest().getHead();}
    
    public X3 get2() {return getRest().getRest().getHead();}
}
