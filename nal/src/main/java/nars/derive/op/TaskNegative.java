package nars.derive.op;

import nars.control.Derivation;
import org.jetbrains.annotations.NotNull;

/** freq < 0.5 */
public final class TaskNegative extends TaskPositive {

    public static final TaskNegative the = new TaskNegative();

    @Override
    public boolean test(@NotNull Derivation m) {
        return !super.test(m);
    }

}
