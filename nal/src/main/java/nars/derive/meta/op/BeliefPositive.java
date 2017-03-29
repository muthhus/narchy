package nars.derive.meta.op;

import nars.Task;
import nars.derive.meta.AtomicPredicate;
import nars.premise.Derivation;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** belief truth is postiive */
public class BeliefPositive extends AtomicPredicate<Derivation> {

    public static final BeliefPositive thePos = new BeliefPositive();

    @Override
    public boolean test(@NotNull Derivation m) {
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
        public boolean test(@NotNull Derivation m) {
            return !super.test(m);
        }
    }

}