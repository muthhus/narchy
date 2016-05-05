package nars.nal.meta.op;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;


public final class TaskPositive extends AtomicBooleanCondition<PremiseEval> {

    public static final TaskPositive the = new TaskPositive();


    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        Truth t = m.premise.task().truth();
        return (t!=null && t.freq() > 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
