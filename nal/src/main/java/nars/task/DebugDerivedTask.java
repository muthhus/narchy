package nars.task;

import nars.Task;
import nars.premise.Derivation;
import nars.premise.Premise;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DebugDerivedTask extends DerivedTask {

    public final Premise premise;

    public DebugDerivedTask(@NotNull Compound tc, byte punct, @Nullable Truth truth, @NotNull Derivation d, long start, long end) {
        super(tc, punct, truth, d, start, end);
        this.premise = d.premise;
    }

    @Nullable
    public final Task getParentTask() {
        return this.premise.task;
    }

    @Nullable
    public final Task getParentBelief() {
        return this.premise.belief;
    }

}
