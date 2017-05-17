package nars.premise;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.pri.Priority;
import nars.$;
import nars.Task;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * prioritizes derivations exhibiting polarization (confident and discerning)
 * and low complexity
 */
public class PreferSimpleAndPolarized implements DerivationBudgeting {

    public final FloatParam polarityFactor = new FloatParam(0.75f, 0f, 1f);

    public final FloatParam belief = new FloatParam(1f, 0f, 2f);
    public final FloatParam goal = new FloatParam(1f, 0f, 2f);
    public final FloatParam question = new FloatParam(1f, 0f, 2f);
    public final FloatParam quest = new FloatParam(1f, 0f, 2f);

    final FloatParam puncFactor(byte punc) {
        switch (punc) {
            case BELIEF: return belief;
            case GOAL: return goal;
            case QUESTION: return question;
            case QUEST: return quest;

            default:
                throw new UnsupportedOperationException();
        }
    }

    public final FloatParam structural = new FloatParam(1f, 0f, 1f);
    public final FloatParam causal = new FloatParam(1f, 0f, 1f);

    final FloatParam opFactor(Compound c) {
        switch (c.op()) {

            case IMPL:
            case CONJ:
            case EQUI:
                return causal;

            case INH:
            case SIM:
            default:
                return structural;
        }
    }


    @Override
    public float budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc, long start, long end) {

        float p = d.premise.pri();

        if (truth!=null) { //belief and goal:
            float polarityFactor = this.polarityFactor.floatValue();
            p *= (1f-polarityFactor) + polarityFactor * truth.polarization();
        } /*else {
            p *= complexityFactorAbsolute(conclusion, punc, d.task, d.belief);
        }*/

        p *= complexityFactorRelative(conclusion, punc, d.task, d.belief);


        p *= puncFactor(punc).floatValue();

        p *= opFactor(conclusion).floatValue();

        return p;
    }

//    static float polarizationFactor(@Nullable Truth truth) {
//        float f = truth.freq();
//        float polarization = 2f * Math.max(f - 0.5f, 0.5f - f);
//        return Math.max(0.5f, polarization);
//    }

    /**
     * occam's razor: penalize relative complexity growth
     * @return a value between 0 and 1 that priority will be scaled by
     */
    public static float complexityFactorAbsolute(Compound conclusion, byte punc, Task task, Task belief) {
        //return 1f / (1f + conclusion.complexity());
        return 1f / (1f + (float)Math.sqrt(
                conclusion.complexity()
        ));
    }

    /**
     * occam's razor: penalize relative complexity growth
     * @return a value between 0 and 1 that priority will be scaled by
     */
    public static float complexityFactorRelative(Compound conclusion, byte punc, Task task, Task belief) {
        int parentComplexity;
        int taskCompl = task.complexity();
        if (belief != null) // && parentBelief.complexity() > parentComplexity)
        {
            int beliefCompl = belief.complexity();
            parentComplexity =
                    //(int)Math.ceil(UtilityFunctions.aveAri(taskCompl, beliefCompl));
                    Math.max(taskCompl, beliefCompl);
        } else {
            parentComplexity = taskCompl;
        }

        int derivedComplexity = conclusion.complexity();

        //controls complexity decay rate
//        int penaltyComplexity;
//        if (punc == QUESTION || punc == QUEST) {
//            //for questions, penalize more by including the parentComplexity in the denominator
//            penaltyComplexity =
//                    //parentComplexity;
//                    taskCompl + beliefCompl;
//        } else {
//            penaltyComplexity = 1;
//        }
        int derivationPenalty = 0;
        return
                //Util.sqr(Util.unitize( //sharpen

            1f -
                //(float)Math.sqrt(
                Util.sqr(
                    Util.unitize(
                        ((float)derivedComplexity) / (derivationPenalty + parentComplexity + derivedComplexity)
                    )
                    //)
                    //Math.max(0, (float)(derivedComplexity - parentComplexity) / (derivationPenalty + parentComplexity + derivedComplexity))
                    //((float) parentComplexity) / (parentComplexity + derivedComplexity)
                    //Util.unitize( 1f / (1f + Math.max(0, (derivedComplexity - parentComplexity)) ))
                )
            ;
    }

//    protected float confidencePreservationFactor(@NotNull Truth truth, @NotNull Derivation conclude) {
//        float w = truth.evi(dur);
//        return 1f - unitize(
//                (conclude.premiseEvidence - w) / (conclude.premiseEvidence + w)
//            //truth.conf() / w2c( conclude.premiseEvidence )
//        );
//    }

}
