package nars.control.premise;

import jcog.Util;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.truth.TruthFunctions.w2c;

/**
 * prioritizes derivations exhibiting polarization (confident and discerning)
 * and low complexity
 * see: http://sciencing.com/calculate-growth-rate-percent-change-4532706.html
 */
public class PreferSimpleAndPolarized implements DerivationBudgeting {

    /** value between 0 and 1.0, range for adjusting polarization (1f has no effect) */
    final float minPolarizationFactor = 0.5f;

//    /** preference for polarity (includes conf) */
//    public final FloatParam polarity = new FloatParam(0.5f, 0f, 1f);
//
//    /** preference for simplicity */
//    public final FloatParam simplicity = new FloatParam(0.5f, 0f, 1f);

//    public final FloatParam belief = new FloatParam(1f, 0f, 2f);
//    public final FloatParam goal = new FloatParam(1f, 0f, 2f);
//    public final FloatParam question = new FloatParam(1f, 0f, 2f);
//    public final FloatParam quest = new FloatParam(1f, 0f, 2f);
//
//    final FloatParam puncFactor(byte punc) {
//        switch (punc) {
//            case BELIEF: return belief;
//            case GOAL: return goal;
//            case QUESTION: return question;
//            case QUEST: return quest;
//
//            default:
//                throw new UnsupportedOperationException();
//        }
//    }
//
//    public final FloatParam structural = new FloatParam(1f, 0f, 1f);
//    public final FloatParam causal = new FloatParam(1f, 0f, 1f);
//
//    final FloatParam opFactor(Compound c) {
//        switch (c.op()) {
//
//            case IMPL:
//            case CONJ:
//            case EQUI:
//                return causal;
//
//            case INH:
//            case SIM:
//            default:
//                return structural;
//        }
//    }


    @Override
    public float budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc, long start, long end) {

        float p =
            d.parentPri;
            //d.premise.pri();

        float simplicityFactor = simplicityFactorRelative(conclusion, punc, d.task, d.beliefTerm);
        if (truth != null) { //belief and goal:
            p *= simplicityFactor;
            float freqFactor = Util.lerp(polarization(truth.freq()), minPolarizationFactor, 1);
            p *= freqFactor;
            float confFactor = evidencePreservationRelative(truth, d);
            p *= confFactor;

//                    simplicity.floatValue() * simplicityFactorRelative(conclusion, punc, d.task, d.belief) +
//                 polarity.floatValue()   * truth.polarization();
        } else {
            //p *= complexityFactorAbsolute(conclusion, punc, d.task, d.belief);
            p *= (simplicityFactor * simplicityFactor * simplicityFactor); // * simplicityFactor * simplicityFactor);
        }

//        p *= puncFactor(punc).floatValue();
//        p *= opFactor(conclusion).floatValue();

        return p;
    }

    /**
     * returns a value between 0.5 and 1.0 relative to the polarity of the frequency
     */
    float polarization(float freq) {
        return Math.abs(freq - 0.5f) * 2f;
    }

//    static float polarizationFactor(@Nullable Truth truth) {
//        float f = truth.freq();
//        float polarization = 2f * Math.max(f - 0.5f, 0.5f - f);
//        return Math.max(0.5f, polarization);
//    }

    /**
     * occam's razor: penalize relative complexity growth
     *
     * @return a value between 0 and 1 that priority will be scaled by
     */
    public static float complexityFactorAbsolute(Compound conclusion, byte punc, Task task, Task belief) {
        //return 1f / (1f + conclusion.complexity());
        return 1f / (1f + (float) Math.sqrt(
                conclusion.complexity()
        ));
    }

    /**
     * occam's razor: penalize relative complexity growth
     *
     * @return a value between 0 and 1 that priority will be scaled by
     */
    public static float simplicityFactorRelative(Compound conclusion, byte punc, Task task, @NotNull Term belief) {
        float premCmp =
                punc == BELIEF || punc == GOAL ?
                        task.complexity() + belief.complexity()
                        :
                        Math.max(task.complexity(), belief.complexity()) //questions, more strict
                ;

        float concCmp = conclusion.complexity();
        return (premCmp / (premCmp + concCmp));
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
//        float complexityGrowth = (combinedComplexity - premiseComplexity) /
//                ((combinedComplexity + premiseComplexity) / 2f);
//        return
//            1f / (1f + Math.max(0, complexityGrowth))
//
//        ;
    }

    protected float evidencePreservationRelative(@NotNull Truth truth, @NotNull Derivation d) {
        float concEvi = truth.conf();
        float premiseEvi = d.premiseEvi > 0 ? w2c(d.premiseEvi) : 0;
        float evidenceShrink = (premiseEvi - concEvi) / ((premiseEvi + concEvi) / 2f);
        return 1f / (1f + Math.max(0, evidenceShrink));
    }

}
