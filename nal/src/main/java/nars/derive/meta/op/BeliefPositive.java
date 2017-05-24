package nars.derive.meta.op;

import nars.Task;
import nars.derive.meta.AtomicPred;
import nars.premise.Derivation;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** belief truth is postiive */
public class BeliefPositive extends AtomicPred<Derivation> {

    public static final BeliefPositive thePos = new BeliefPositive();
    public static final BeliefNegative theNeg = new BeliefNegative();

    @Override
    public boolean test(@NotNull Derivation m) {
        Task B = m.belief;
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


        @Override
        public boolean test(@NotNull Derivation m) {
            return !super.test(m);
        }
    }

}