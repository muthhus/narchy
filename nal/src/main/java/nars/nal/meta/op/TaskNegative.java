package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** freq < 0.5 */
public final class TaskNegative extends AtomicBoolCondition {

    public static final TaskNegative the = new TaskNegative();


    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        Truth t = m.premise.task().truth();
        return (t!=null && t.freq() < 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }


}
