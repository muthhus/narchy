package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** task truth is postiive */
public class TaskPositive extends AtomicBoolCondition {

    public static final TaskPositive the = new TaskPositive();

    @Override
    public boolean run(@NotNull PremiseEval m, int now) {
        Truth t = m.premise.task.truth();
        return (t!=null && t.freq() >= 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
