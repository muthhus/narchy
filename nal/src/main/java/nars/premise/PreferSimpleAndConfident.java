package nars.premise;

import jcog.Util;
import jcog.data.FloatParam;
import nars.$;
import nars.Task;
import nars.budget.Budget;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.unitize;
import static nars.Op.*;
import static nars.truth.TruthFunctions.w2c;
import static nars.util.UtilityFunctions.and;

/**
 * prioritizes derivations exhibiting confidence increase, relative to the premise's evidence
 */
public class PreferSimpleAndConfident implements DerivationBudgeting {


    public final FloatParam belief = new FloatParam(1f, 0f, 1f);
    public final FloatParam goal = new FloatParam(1f, 0f, 1f);
    public final FloatParam question = new FloatParam(1f, 0f, 1f);
    public final FloatParam quest = new FloatParam(1f, 0f, 1f);

    public final FloatParam puncFactor(byte punc) {
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

    public final FloatParam opFactor(Compound c) {
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
    public Budget budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc) {

        float q = d.premise.qua();
        final float quaMin = d.quaMin;



        if (truth!=null) { //belief and goal:
            q *= confidencePreservationFactor(truth, d);
            if (q < quaMin) return null;
        }

        q *= complexityFactor(conclusion, punc, d.task, d.belief);
        if (q < quaMin) return null;

        float p = d.premise.pri();
        p *= puncFactor(punc).floatValue();

        FloatParam off = opFactor(conclusion);
        p *= off.floatValue();

        p *= q; //further discount priority

        return $.b(p, q);
    }

    /**
     * occam's razor: penalize relative complexity growth
     *
     * @return a value between 0 and 1 that priority will be scaled by
     */
    public static float complexityFactor(Compound conclusion, byte punc, Task task, Task belief) {
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

    protected float confidencePreservationFactor(@NotNull Truth truth, @NotNull Derivation conclude) {
        return unitize(truth.conf() / w2c( conclude.premiseEvidence ));
    }

}
