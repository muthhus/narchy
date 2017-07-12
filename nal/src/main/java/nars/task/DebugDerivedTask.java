package nars.task;

import nars.Task;
import nars.control.premise.Derivation;
import nars.derive.rule.PremiseRule;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DebugDerivedTask extends DerivedTask {


    private final Task parentBelief;
    private final Task parentTask;

    public DebugDerivedTask(@NotNull Compound tc, byte punct, @Nullable Truth truth, @NotNull Derivation d, long start, long end, @NotNull short rule) {
        super(tc, punct, truth, d, start, end, rule);
        this.parentTask = d.task;
        this.parentBelief = d.belief;
    }

    @Override
    @Nullable
    public final Task getParentTask() {
        return parentTask;
    }

    @Override
    @Nullable
    public final Task getParentBelief() {
        return parentBelief;
    }

}
