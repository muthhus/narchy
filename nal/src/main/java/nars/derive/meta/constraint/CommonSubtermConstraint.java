package nars.derive.meta.constraint;

import nars.$;
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

    @NotNull
    @Override
    protected boolean invalid(@NotNull Compound x, @NotNull Compound y) {
        //return x.op().var || y.containsTermRecursively(x);
        return x.equals(y) || !TermContainer.commonSubtermsRecurse(x, y, true);
    }


}
