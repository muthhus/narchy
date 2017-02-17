package nars.premise;

import nars.$;
import nars.Task;
import nars.budget.Budget;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.unitize;

/**
 * Created by me on 2/6/17.
 */
abstract public class DefaultPremise extends Premise {

    public DefaultPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
        super(c, task, beliefTerm, belief, pri, qua);
    }


    protected Budget budget(float priFactor, float quaFactor, float quaMin) {
        float q = qua() * quaFactor;
        if (q < quaMin)
            return null;
        else
            return $.b(pri() * priFactor, q);
    }

    @Override
    public @Nullable Budget budget(@NotNull Compound conclusion, @Nullable Truth truth, @NotNull Derivation conclude) {


        float quaMin = conclude.quaMin;
        if (truth!=null) {
            //belief and goal:
            float quaFactor = unitize(qualityFactor(truth, conclude));
            if (quaFactor!=quaFactor)
                throw new Budget.BudgetException("NaN quality during premise build");

            //early termination test to avoid calculating priFactor:
            if (quaFactor * qua() < quaMin)
                return null;

            return budget(
                    unitize(priFactor(truth, conclusion, task, belief)),
                    quaFactor,
                    quaMin);
        } else {
            //question and quest
            float priFactor = unitize(priFactor(truth, conclusion, task, belief));
            return budget(
                    priFactor,
                    priFactor,
                    quaMin);
        }

    }

    abstract float qualityFactor(@NotNull Truth truth, @NotNull Derivation conclude);

    abstract float priFactor(@Nullable Truth truth, @NotNull Compound conclusion, @NotNull Task task, @Nullable Task belief);

}
