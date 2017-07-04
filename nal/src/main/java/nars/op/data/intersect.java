package nars.op.data;

import nars.Op;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

public class intersect extends Functor.BinaryFunctor {

    public intersect() {
        super("intersect");
    }

    @Override public Term apply(@NotNull Term a, @NotNull Term b) {
        if (a instanceof Compound && b instanceof Compound) {
            Op aop = a.op();
            if (b.op() == aop)
                return Terms.intersect(aop, (Compound) a, (Compound) b);
        }

        return null;
    }


}
