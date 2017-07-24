package nars.derive.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
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
        return true;
    }

    @NotNull
    @Override
    protected boolean invalid(@NotNull Compound x, @NotNull Compound y) {
        return !TermContainer.hasCommonSubtermsRecursive(x, y, true);
    }
    @NotNull
    @Override protected boolean invalid(Term x, Compound y) {
        return x instanceof Variable || !y.contains(x);
    }


}
