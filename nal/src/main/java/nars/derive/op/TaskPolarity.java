package nars.derive.op;

import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * task truth is postiive
 */
public class TaskPolarity {

    public static final PrediTerm<Derivation> pos = new AbstractPred<Derivation>("(TaskPos)") {
        @Override
        public boolean test(@NotNull Derivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() >= 0.5f);
        }

    };
    public static final PrediTerm<Derivation> neg = new AbstractPred<Derivation>("(TaskNeg)") {
        @Override
        public boolean test(@NotNull Derivation m) {
            Truth t = m.taskTruth;
            return (t != null && t.freq() < 0.5f);
        }
    };


}
