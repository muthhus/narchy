package nars.derive.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

/**
 * (recursive)
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
    protected boolean invalid(@NotNull Compound x, @NotNull Compound y) {
        return !TermContainer.commonSubtermsRecurse(x, y, true);
    }
    @Override protected boolean invalid(Term x, Compound y) {

        return y.contains(x);
    }


}
