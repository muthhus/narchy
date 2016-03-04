package alice.tuprologx.pj.model;

public class Nil extends Compound<Nil> {
    @Override
    public int arity() {return 0;}
       
        
    @Override
    public <Z> Z/*Object*/ toJava() {
        throw new UnsupportedOperationException();
    }

    @Override
    public alice.tuprolog.Term marshal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return null;
    }
}

