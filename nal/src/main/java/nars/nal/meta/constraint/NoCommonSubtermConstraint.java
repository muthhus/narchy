package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.FindSubst;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import static nars.term.container.TermContainer.subtermOfTheOther;

/** ensures the compared terms are not equal and share no (1st-level only) terms in common.  variables excluded */
public class NoCommonSubtermConstraint implements MatchConstraint {

    @NotNull
    protected final Term b;

    public NoCommonSubtermConstraint(@NotNull Term b) {
        this.b = b;
    }


    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull FindSubst f) {
        if (y instanceof Variable)
            return false;

        Term B = f.xy(b);

        if (B == null || B instanceof Variable)
            return false;

        boolean bCompound = B instanceof Compound;

        if (B.equals(y))
            return true;

        if (y instanceof Compound) {

            Compound C = (Compound) y;

            return bCompound ?
                    invalid((Compound) B, C)
                    :
                    C.containsTerm(B);

        } else {

            return bCompound ?
                    B.containsTerm(y)
                    :
                    false; //B.equals(y);
        }

    }

    /** comparison between two compounds */
    @NotNull protected boolean invalid(Compound x, Compound y) {
        return subtermOfTheOther(x, y, true);
    }

                 //commonSubtermsRecurse((Compound) B, C, true, new HashSet())
                    //commonSubterms((Compound) B, C, true, scratch.get())

    @NotNull
    @Override
    public String toString() {
        return "neqCom(" + b + ')';
    }

}
