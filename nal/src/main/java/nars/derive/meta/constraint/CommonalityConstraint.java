package nars.derive.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

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
    public final boolean invalid(@NotNull Term y, @NotNull Unify f) {

        if (y instanceof Variable)
            return false;

        Term x = f.xy(other);

        if (x!=null) {

            boolean result;

            if (x.equals(y))
                result = true; //match
            else if (x instanceof Variable)
                result = false; //only if x!=y
            else {

                boolean bCompound = x instanceof Compound;
                if (!(y instanceof Compound)) {
                    result = bCompound && x.contains(y); //B.equals(y);
                } else {

                    Compound C = (Compound) y;

                    result = bCompound ?
                            invalid((Compound) x, C)
                            :
                            invalid(/*(Term)*/x, C);

                }
            }


            return result;
        }

        return false;
    }

    @NotNull protected abstract boolean invalid(Compound x, Compound y);

    @NotNull protected abstract boolean invalid(Term x, Compound y);
}
