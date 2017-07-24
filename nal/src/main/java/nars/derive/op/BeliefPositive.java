package nars.derive.op;

import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** belief truth is postiive */
public class BeliefPositive extends AbstractPred<Derivation> {

    public static final BeliefPositive beliefPos = new BeliefPositive();
    public static final BeliefNegative beliefNeg = new BeliefNegative();
    public static final BeliefExists beliefExists = new BeliefExists();

    public BeliefPositive() {
        this("belief(positive)");
    }

    BeliefPositive(String id) {
        super(id);
    }

    @Override
    public boolean test(@NotNull Derivation m) {
        Truth B = m.beliefTruth;
        if (B !=null) {
            return B.freq() >= 0.5f;
        }
        return false;
    }


    public static final class BeliefNegative extends BeliefPositive {

        public BeliefNegative() {
            super("belief(negative)");
        }

        @Override
        public boolean test(@NotNull Derivation m) {
            return !super.test(m);
        }
    }

    public static final class BeliefExists extends BeliefPositive {

        public BeliefExists() {
            super("belief(exists)");
        }

        @Override
        public boolean test(@NotNull Derivation m) {
            return m.belief!=null;
        }
    }

}