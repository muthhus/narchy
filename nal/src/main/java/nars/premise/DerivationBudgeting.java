package nars.premise;

import jcog.pri.Priority;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * determines budget of derived tasks
 */
public interface DerivationBudgeting {

    /** return null to cancel a derivation */
    Priority budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc, long start, long end);

}
