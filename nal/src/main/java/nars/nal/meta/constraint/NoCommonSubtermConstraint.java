package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import static nars.term.container.TermContainer.subtermOfTheOther;

/** ensures the compared terms are not equal and share no (1st-level only, or recursively) terms in common.
 *  variables excluded */
public final class NoCommonSubtermConstraint extends CommonalityConstraint {

    private final boolean recurse;

    public NoCommonSubtermConstraint(@NotNull Term b, boolean recurse) {
        super(b);
        this.recurse = recurse;
    }

    /** comparison between two compounds */
    @Override
    @NotNull protected final boolean invalid(Compound x, Compound y) {

        return subtermOfTheOther(x, y, recurse, true);
    }

                 //commonSubtermsRecurse((Compound) B, C, true, new HashSet())
                    //commonSubterms((Compound) B, C, true, scratch.get())

    @NotNull
    @Override
    public String toString() {
        return "neqCom(" + b + (recurse ? "R)" :  ")");
    }

}
