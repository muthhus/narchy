package nars.premise;

import jcog.Util;
import nars.$;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 2/6/17.
 */
public class DefaultPremise extends Premise {

    public DefaultPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
        super(c, task, beliefTerm, belief, pri, qua);
    }


    @Override
    public @Nullable Budget budget(@NotNull Term conclusion, @Nullable Truth truth, @NotNull Derivation conclude) {

        float truthFactor = Util.unitize(qualityFactor(truth, conclude));
        float complexityFactor =
                BudgetFunctions.occamComplexityGrowthRelative(conclusion, task, belief, 1);

        final float q = qua() * complexityFactor;

        if (q < conclude.quaMin)
            return null;

        float p = pri() * truthFactor * complexityFactor;


        return $.b(p, Util.clamp(q, 0f, 1f - Param.BUDGET_EPSILON));

    }

    protected float qualityFactor(@Nullable Truth truth, @NotNull Derivation conclude) {
        return 1f;
    }
}
