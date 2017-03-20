package nars.premise;

import nars.budget.Budget;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * determines budget of derived tasks
 */
public interface DerivationBudgeting {

    /** return null to cancel a derivation */
    @Nullable Budget budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc);

}
