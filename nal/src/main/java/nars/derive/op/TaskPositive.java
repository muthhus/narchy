package nars.derive.op;

import nars.control.premise.Derivation;
import nars.derive.AtomicPred;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** task truth is postiive */
public class TaskPositive extends AtomicPred<Derivation> {

    public static final TaskPositive the = new TaskPositive();

    @Override
    public boolean test(@NotNull Derivation m) {
        Truth t = m.taskTruth;
        return (t!=null && t.freq() >= 0.5f);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
