package nars.derive.constraint;

import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

/**
 * invalid if any of the following:
 *      terms are equal
 *      a term contains the other
 *      the terms have no non-variable subterms in common
 */
public final class CommonSubtermConstraint extends CommonalityConstraint {

    public CommonSubtermConstraint(Term target, @NotNull Term x) {
        super("neqAndCom", target, x);
    }

    @Override
    public int cost() {
        return 10;
    }

    @Override
    protected boolean invalid(Term x, Term y) {

        return x.equals(y) || x.containsRecursively(y) || y.containsRecursively(x) || !TermContainer.hasCommonSubtermsRecursive(x, y, true);
    }

}
