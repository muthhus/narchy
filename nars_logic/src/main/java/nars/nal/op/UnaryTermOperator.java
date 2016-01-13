package nars.nal.op;

import nars.term.Term;
import nars.term.compile.TermBuilder;
import nars.term.compound.Compound;

/**
 * Created by me on 12/12/15.
 */
public abstract class UnaryTermOperator extends ImmediateTermTransform {

    @Override public final Term function(Compound x, TermBuilder i) {
        if (x.size()<1)
            throw new RuntimeException(this + " requires >= 2 args");

        return apply(x.term(0), i);
    }

    public abstract Term apply(Term a, TermBuilder i);
}
