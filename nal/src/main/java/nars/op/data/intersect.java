package nars.op.data;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.Functor;
import org.jetbrains.annotations.NotNull;

public class intersect extends Functor.BinaryFunctor {

    public intersect() {
        super("intersect");
    }

    @NotNull
    @Override public Term apply(@NotNull Term a, @NotNull Term b) {
        if (a instanceof Compound && b instanceof Compound)
            return $.terms.intersect(a.op(), (Compound)a, (Compound) b);
        else
            return null;
    }


}
