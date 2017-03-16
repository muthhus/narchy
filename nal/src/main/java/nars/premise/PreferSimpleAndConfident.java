package nars.premise;

import jcog.Util;
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
import static nars.Op.QUEST;
import static nars.Op.QUESTION;
import static nars.truth.TruthFunctions.w2c;
import static nars.util.UtilityFunctions.and;

/**
 * prioritizes derivations exhibiting confidence increase, relative to the premise's evidence
 */
public class PreferSimpleAndConfident implements DerivationBudgeting {


    @Override
    public Budget budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc) {


        float quaMin = d.quaMin;
        float quaFactor;
        if (truth!=null) {
            //belief and goal:
            quaFactor = unitize(qualityFactor(truth, d));
        } else {
            quaFactor = 0.5f;
        }

        float c = unitize(complexityDiscount(conclusion, punc, d.task, d.belief));
        quaFactor *= c;

        //early termination test to avoid calculating priFactor:
        float q = quaFactor * d.premise.qua();
        if (q < quaMin)
            return null;

        return $.b( d.premise.pri(), q);
    }

    /**
     * occam's razor: penalize relative complexity growth
     *
     * @return a value between 0 and 1 that priority will be scaled by
     */
    public static float complexityDiscount(Compound conclusion, byte punc, Task task, Task belief) {
        int parentComplexity;
        int taskCompl = task.complexity();
        int beliefCompl;
        if (belief != null) // && parentBelief.complexity() > parentComplexity)
        {
            beliefCompl = belief.complexity();
            parentComplexity =
                    Math.min(taskCompl, beliefCompl);
        } else {
            beliefCompl = 0;
            parentComplexity = taskCompl;
        }

        int derivedComplexity = conclusion.complexity();

        //controls complexity decay rate
        int penaltyComplexity;
        if (punc == QUESTION || punc == QUEST) {
            //for questions, penalize more by including the parentComplexity in the denominator
            penaltyComplexity =
                    //parentComplexity;
                    taskCompl + beliefCompl;
        } else {
            penaltyComplexity = 1;
        }
        return
                //Util.sqr( //sharpen
                    Util.unitize( ((float) parentComplexity) / (penaltyComplexity + derivedComplexity))
                //)
            ;
    }

    protected float qualityFactor(@NotNull Truth truth, @NotNull Derivation conclude) {
        return and(truth.conf(), w2c( conclude.premiseEvidence ));
    }

}
