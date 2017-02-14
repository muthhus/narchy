package nars.derive.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 10/16/16.
 */
public abstract class CommonalityConstraint implements MatchConstraint {
    @NotNull
    protected final Term b;

    public CommonalityConstraint(@NotNull Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull Unify f) {
        if (y instanceof Variable)
            return false;

        Term bb = f.xy(b);

        if (bb == null || bb instanceof Variable)
            return false;

        if (bb.equals(y))
            return true;

        boolean bCompound = bb instanceof Compound;
        if (!(y instanceof Compound)) {

            return bCompound && bb.containsTerm(y); //B.equals(y);
        } else {

            Compound C = (Compound) y;

            return bCompound ?
                    invalid((Compound) bb, C)
                    :
                    C.containsTerm(bb);

        }

    }

    @NotNull protected abstract boolean invalid(Compound x, Compound y);
}
