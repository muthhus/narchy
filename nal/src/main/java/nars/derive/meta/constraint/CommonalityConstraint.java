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

        Term B = f.xy(b);

        if (B == null || B instanceof Variable)
            return false;

        boolean bCompound = B instanceof Compound;

        if (B.equals(y))
            return true;

        if (!(y instanceof Compound)) {

            return bCompound && B.containsTerm(y); //B.equals(y);
        } else {

            Compound C = (Compound) y;

            return bCompound ?
                    invalid((Compound) B, C)
                    :
                    C.containsTerm(B);

        }

    }

    @NotNull protected abstract boolean invalid(Compound x, Compound y);
}
