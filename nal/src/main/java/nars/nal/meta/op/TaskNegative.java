package nars.nal.meta.op;

import nars.nal.meta.Derivation;
import org.jetbrains.annotations.NotNull;

/** freq < 0.5 */
public final class TaskNegative extends TaskPositive {

    public static final TaskNegative the = new TaskNegative();

    @Override
    public boolean run(@NotNull Derivation m, int now) {
        return !super.run(m, now);
    }

}
