package nars.derive.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 10/16/16.
 */
public abstract class CommonalityConstraint extends MatchConstraint {

    private final Term other;

    public CommonalityConstraint(String func, Term target, Term other) {

        super(func, target, other);
        this.other = other;
    }

    @Override
    public boolean invalid(@NotNull Term y, @NotNull Unify f) {

        Term x = f.xy(other);
        if (x!=null) {
            boolean bCompound = x instanceof Compound;
            if ((y instanceof Compound)) {

                Compound C = (Compound) y;

                return bCompound ?
                        invalid((Compound) x, C)
                        :
                        invalid(/*(Term)*/x, C);

            } else if (!(y instanceof Variable)) {
                return bCompound && x.containsTerm(y); //B.equals(y);
            } else {
                //probably a variable
            }
        }

        return false;
    }

    @NotNull protected abstract boolean invalid(Compound x, Compound y);

    @NotNull protected boolean invalid(Term x, Compound y) {
        return y.containsTerm(x);
    }
}
