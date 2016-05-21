package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/* freq >= 0.5 */
public final class BeliefPositive extends AtomicBoolCondition {

    public static final BeliefPositive the = new BeliefPositive();


    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        Task b = m.premise.belief();
        return (b != null && b.freq() >= 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
