package nars.derive.meta;

import nars.control.premise.Derivation;
import nars.term.Compound;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
public final class SolvePuncFromTask extends Solve {

    public SolvePuncFromTask(Compound i, Conclude der, TruthOperator belief, TruthOperator desire, boolean beliefProjected) {
        super(i, der, belief, desire, beliefProjected);
    }

    @Override
    public boolean test(@NotNull Derivation m) {
        return measure(m, m.taskPunct);
    }
}
