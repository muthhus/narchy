package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 10/31/16.
 */
public final class CommonSubtermConstraint extends CommonalityConstraint {

    public CommonSubtermConstraint(@NotNull Term b) {
        super(b);
    }

    @NotNull
    @Override
    protected boolean invalid(@NotNull Compound x, @NotNull Compound y) {
        return /*super.invalid(x, y) ||*/ !TermContainer.commonSubtermsRecurse(x, y, false);
    }

    @NotNull
    @Override
    public String toString() {
        return "neqAndCom(" + b + ")";
    }

}
