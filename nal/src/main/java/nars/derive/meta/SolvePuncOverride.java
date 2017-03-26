package nars.derive.meta;

import nars.premise.Derivation;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
public final class SolvePuncOverride extends Solve {
    private final byte puncOverride;


    public SolvePuncOverride(String i, Conclude der, byte puncOverride, TruthOperator belief, TruthOperator desire, boolean beliefProjected) {
        super(i, der, belief, desire, beliefProjected);
        this.puncOverride = puncOverride;
    }

    @Override
    public boolean test(@NotNull Derivation m) {
        return measure(m, puncOverride);
    }
}
