package nars.op.data;

import nars.Op;
import nars.term.Functor;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * all X which are in the first term AND not in the second term
 */
public class differ extends Functor.BinaryFunctor {

    public differ() {
        super("differ");
    }

    @Override
    public boolean validOp(Op o) {
        return o.commutative;
    }

    @Override
    public Term apply(@NotNull Term a, @NotNull Term b) {

        Term y = Op.difference(a.op(), a, b);
//        if (y.equals(a))
//            return Null; //prevent identical fall-through
        return y;

    }
}
