package nars.nal.op;

import nars.nal.meta.PremiseEval;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
public final class SolvePuncFromTask extends Solve {

    public SolvePuncFromTask(String i, Derive der, TruthOperator belief, TruthOperator desire) {
        super(i, der, belief, desire);
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        return measure(m, m.punct.get());
    }
}
