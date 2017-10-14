package nars.op.data;

import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

public class intersect extends Functor.BinaryFunctor {

    public static final intersect the = new intersect();

    intersect() {
        super("intersect");
    }

    @Override
    public boolean validOp(Op o) {
        return o.commutative;
    }

    @Override
    public Term apply(@NotNull Term a, @NotNull Term b) {
//        Op aop = a.op();
//        if (b.op() == aop)
            return Terms.intersect(a.op(), a.subterms(), b.subterms());
//        else
//            return null;
    }


}
