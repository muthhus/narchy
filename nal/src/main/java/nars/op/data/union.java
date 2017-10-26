package nars.op.data;

import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.Nullable;

public class union extends Functor.BinaryFunctor {

    public static final union the = new union();

    union() {
        super("union");
    }

    @Override
    public boolean validOp(Op o) {
        return o.commutative;
    }

    @Nullable
    @Override public Term apply(Term a, Term b) {

        return Terms.union(a.op(), a.subterms(), b.subterms() );
    }

}
