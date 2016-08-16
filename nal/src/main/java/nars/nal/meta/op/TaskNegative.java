package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/** freq < 0.5 */
public final class TaskNegative extends TaskPositive {

    public static final TaskNegative the = new TaskNegative();


    @Override
    public boolean booleanValueOf(@NotNull PremiseEval m) {
        return !super.booleanValueOf(m);
    }


}
