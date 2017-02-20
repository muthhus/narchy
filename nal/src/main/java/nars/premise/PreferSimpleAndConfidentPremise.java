package nars.premise;

import jcog.Util;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.w2c;
import static nars.util.UtilityFunctions.and;

/**
 * prioritizes derivations exhibiting confidence increase, relative to the premise's evidence
 */
public class PreferSimpleAndConfidentPremise extends DefaultPremise {

    public PreferSimpleAndConfidentPremise(Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
        super(c, task, beliefTerm, belief, pri, qua);
    }

    /**
     * occam's razor: penalize relative complexity growth
     *
     * @return a value between 0 and 1 that priority will be scaled by
     */
    @Override
    float priFactor(@Nullable Truth truth, Compound conclusion, Task task, Task belief) {

        int parentComplexity;
        int taskCompl = task.complexity();
        int beliefCompl;
        if (belief != null) // && parentBelief.complexity() > parentComplexity)
        {
            beliefCompl = belief.complexity();
            parentComplexity =
                    Math.max(taskCompl, beliefCompl);
        } else {
            beliefCompl = 0;
            parentComplexity = taskCompl;
        }

        int derivedComplexity = conclusion.complexity();

        //controls complexity decay rate
        int penaltyComplexity;
        if (truth != null) {
            penaltyComplexity = 1;
        } else {
            //for questions, penalize more by including the parentComplexity in the denominator
            penaltyComplexity =
                    //parentComplexity;
                    taskCompl + beliefCompl;
        }
        return
                //Util.sqr( //sharpen
                    Util.unitize( ((float) parentComplexity) / (penaltyComplexity + derivedComplexity))
                //)
            ;
    }

    @Override
    protected float qualityFactor(@NotNull Truth truth, @NotNull Derivation conclude) {

        float pe = conclude.premiseEvidence;
        if (pe == 0)
            return 0; //??
        //return truth.conf() / w2c(pe);
        return and(truth.conf(), w2c(pe));

    }

}
