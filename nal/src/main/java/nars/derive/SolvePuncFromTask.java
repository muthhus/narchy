package nars.derive;

import nars.control.Derivation;
import nars.term.Compound;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
public final class SolvePuncFromTask extends Solve {

    public SolvePuncFromTask(Compound i, TruthOperator belief, TruthOperator desire, boolean beliefProjected) {
        super(i, belief, desire, beliefProjected);
    }

    @Override
    public boolean test(@NotNull Derivation m) {
        return test(m, m.taskPunct);
    }
}
