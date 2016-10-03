package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.FindSubst;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import static nars.term.container.TermContainer.subtermOfTheOther;

/** ensures the compared terms are not equal and share no (1st-level only) terms in common.  variables excluded */
public class NotSubtermOfConstraint implements MatchConstraint {

    @NotNull
    protected final Term mustNotContain;

    public NotSubtermOfConstraint(@NotNull Term mustNotContain) {
        this.mustNotContain = mustNotContain;
    }


    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull FindSubst f) {
        if (y instanceof Variable)
            return false;

        Term mustNotContain = f.xy(this.mustNotContain);

        return (mustNotContain!=null && (mustNotContain.equals(y) || mustNotContain.containsTerm(y)));
    }


    @NotNull
    @Override
    public String toString() {
        return "neqExc(" + mustNotContain + ')';
    }

}
