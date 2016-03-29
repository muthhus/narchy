package nars.nal.meta.op;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/15/15.
 */
public final class TaskNegative extends AtomicBooleanCondition<PremiseEval> {

    public static final TaskNegative the = new TaskNegative();

    private TaskNegative() {
        super();
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        Task task = m.premise.task();
        return (task.isBeliefOrGoal() && task.freq() < 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }


}
