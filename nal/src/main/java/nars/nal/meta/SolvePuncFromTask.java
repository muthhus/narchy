package nars.nal.meta;

import nars.nal.Derivation;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
public final class SolvePuncFromTask extends Solve {

    public SolvePuncFromTask(String i, Conclude der, TruthOperator belief, TruthOperator desire) {
        super(i, der, belief, desire);
    }

    @Override
    public boolean run(@NotNull Derivation m, int now) {
        return measure(m, m.taskPunct);
    }
}
