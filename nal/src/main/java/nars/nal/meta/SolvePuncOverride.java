package nars.nal.meta;

import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
public final class SolvePuncOverride extends Solve {
    private final char puncOverride;


    public SolvePuncOverride(String i, Conclude der, char puncOverride, TruthOperator belief, TruthOperator desire) {
        super(i, der, belief, desire);
        this.puncOverride = puncOverride;
    }

    @Override
    public boolean run(@NotNull Derivation m, int now) {
        return measure(m, puncOverride);
    }
}
