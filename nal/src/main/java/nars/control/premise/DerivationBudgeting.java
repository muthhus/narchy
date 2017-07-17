package nars.control.premise;

import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * determines budget of derived tasks
 */
@Deprecated  public interface DerivationBudgeting {

    /** return NaN to cancel a derivation */
    float budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc, long start, long end);

}
