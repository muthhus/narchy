package nars.nal.op;

import nars.nal.meta.PremiseEval;
import nars.nal.meta.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
public final class SolvePuncOverride extends Solve {
    private final char puncOverride;


    public SolvePuncOverride(String i, Derive der, char puncOverride, TruthOperator belief, TruthOperator desire) {
        super(i, der, belief, desire);
        this.puncOverride = puncOverride;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        return measure(m, puncOverride);
    }
}
