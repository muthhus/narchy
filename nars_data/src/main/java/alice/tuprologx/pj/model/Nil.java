package alice.tuprologx.pj.model;

import alice.tuprolog.PTerm;

public class Nil extends Compound<Nil> {
    @Override
    public int arity() {return 0;}
       
        
    @Override
    public <Z> Z/*Object*/ toJava() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PTerm marshal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return null;
    }
}

