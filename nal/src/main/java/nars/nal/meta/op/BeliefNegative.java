package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/** freq < 0.5 */
public final class BeliefNegative extends AtomicBoolCondition {

    public static final BeliefNegative the = new BeliefNegative();


    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        Task b = m.premise.belief();
        return (b != null && b.freq() < 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
