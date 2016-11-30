package nars.nal.meta.op;

import nars.Task;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.Derivation;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** belief truth is postiive */
public class BeliefPositive extends AtomicBoolCondition {

    public static final BeliefPositive the = new BeliefPositive();

    @Override
    public boolean run(@NotNull Derivation m, int now) {
        Task B = m.premise.belief;
        if (B !=null) {
            Truth t = B.truth();
            return (t != null && t.freq() >= 0.5f);
        }
        return false;
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public static final class BeliefNegative extends BeliefPositive {

        public static final BeliefNegative the = new BeliefNegative();

        @Override
        public boolean run(@NotNull Derivation m, int now) {
            return !super.run(m, now);
        }
    }

}