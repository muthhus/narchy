package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.Derivation;
import org.jetbrains.annotations.NotNull;

/** matches the possibility that one half of the premise must be contained within the other.
 * this would in theory be more efficient than performing a complete match for the redundancies
 * which we can determine as a precondition of the particular task/belief pair
 * before even beginning the match. */
final class TaskBeliefEqualCondition extends AtomicBoolCondition {

    @Override
    public boolean run(@NotNull Derivation m, int now) {
        return m.taskTerm.equals(m.beliefTerm);
    }

    @Override
    public String toString() {
        return "taskbeliefEq";
    }
}
