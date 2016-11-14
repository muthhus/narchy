package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.term.container.TermContainer.commonSubtermsRecurse;

/** ensures the compared terms are not equal and recursively share no terms in common.  variables excluded */
public final class NoCommonRecursiveSubtermConstraint extends CommonalityConstraint {

    public NoCommonRecursiveSubtermConstraint(@NotNull Term b) {
        super(b);
    }

    /** comparison between two compounds */
    @Override
    @NotNull protected boolean invalid(@NotNull Compound x, @NotNull Compound y) {
        return commonSubtermsRecurse(x, y, true);
    }

    @NotNull
    @Override
    public String toString() {
        return "neqComRec(" + b + ')';
    }
}
