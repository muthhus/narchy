package nars.nal.meta.op;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;


public final class TaskPositive extends AtomicBooleanCondition<PremiseEval> {

    public static final TaskPositive the = new TaskPositive();

    private TaskPositive() {
        super();
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        Task task = m.premise.task();
        return (task.isBeliefOrGoal() && task.freq() > 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
