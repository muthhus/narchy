package nars.nal.meta;

import nars.Memory;
import nars.Premise;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface TruthOperator {
    @Nullable
    Truth apply(Truth task, Truth belief, Memory m);
    boolean allowOverlap();

    default boolean  apply(@NotNull PremiseMatch m) {
        Premise premise = m.premise;
        Truth truth = apply(
                premise.getTask().truth(),
                premise.getBelief() == null ? null : premise.getBelief().truth(),
                premise.memory()
        );

        if (truth!=null) {
            //pre-filter insufficient confidence level
            if (truth.conf() < m.getMinConfidence()) {
                return false;
            }

            m.truth.set(truth);
            return true;
        }
        return false;
    }
}
