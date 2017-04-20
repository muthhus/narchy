package nars.derive.meta.constraint;

import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 10/16/16.
 */
public abstract class CommonalityConstraint extends MatchConstraint {

    public CommonalityConstraint(String func, Term target, Term... args) {
        super(func, target, args);
    }

    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull Unify f) {

        Term bb = f.xy(ref);

        if (bb == null) return false;

        if (bb.equals(y)) return true;
        if (bb instanceof Variable) return false; //only if bb!=y

        boolean bCompound = bb instanceof Compound;
        if (!(y instanceof Compound)) {

            return bCompound && bb.containsTerm(y); //B.equals(y);
        } else {

            Compound C = (Compound) y;

            return bCompound ?
                    invalid((Compound) bb, C)
                    :
                    invalid(/*(Term)*/bb, C);

        }

    }

    @NotNull protected abstract boolean invalid(Compound x, Compound y);

    @NotNull protected boolean invalid(Term x, Compound y) {
        return y.containsTerm(x);
    }
}
