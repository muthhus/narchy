package nars.nal.meta.op;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/5/16.
 */
public final class BeliefNegative extends AtomicBooleanCondition<PremiseEval> {

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
