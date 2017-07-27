package nars.derive.constraint;

import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

/**
 * true if the terms have any non-variable in common
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
        return !TermContainer.hasCommonSubtermsRecursive(x, y, true);
    }

}
