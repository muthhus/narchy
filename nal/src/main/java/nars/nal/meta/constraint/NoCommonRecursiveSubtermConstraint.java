package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

import static nars.term.container.TermContainer.commonSubtermsRecurse;

/** ensures the compared terms are not equal and recursively share no terms in common.  variables excluded */
public final class NoCommonRecursiveSubtermConstraint extends NoCommonSubtermConstraint {

    public NoCommonRecursiveSubtermConstraint(@NotNull Term b) {
        super(b);
    }

    /** comparison between two compounds */
    @NotNull protected boolean invalid(Compound x, Compound y) {
        return commonSubtermsRecurse(x, y, true, new HashSet());
    }

    @NotNull
    @Override
    public String toString() {
        return "neqComRec(" + b + ')';
    }
}
