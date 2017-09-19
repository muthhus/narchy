package nars.derive.op;

import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * task truth is postiive
 */
abstract public class TaskPolarity extends AbstractPred<Derivation>{

    public static final PrediTerm<Derivation> taskPos = new TaskPolarity("TaskPos") {
        @Override
        public boolean test(@NotNull Derivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() >= 0.5f);
        }

    };
    public static final PrediTerm<Derivation> taskNeg = new TaskPolarity("TaskNeg") {
        @Override
        public boolean test(@NotNull Derivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() < 0.5f);
        }
    };
    public static final PrediTerm<Derivation> beliefPos = new TaskPolarity("BeliefPos") {
        @Override public boolean test(Derivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() >= 0.5f;
        }
    };
    public static final PrediTerm<Derivation> beliefNeg = new TaskPolarity("BeliefNeg") {
        @Override public boolean test(Derivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() < 0.5f;
        }
    };

    @Override
    public float cost() {
        return 0.1f;
    }

    protected TaskPolarity(String x) {
        super(x);
    }
}
