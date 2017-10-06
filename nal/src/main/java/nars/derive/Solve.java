package nars.derive;

import jcog.Util;
import nars.Param;
import nars.control.Derivation;
import nars.term.Compound;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static nars.Op.*;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.c2wSafe;

/**
 * Evaluates the truth of a premise
 */
abstract public class Solve extends AbstractPred<Derivation> {

    private final TruthOperator belief;
    private final TruthOperator goal;
    private final boolean beliefProjected;

    Solve(Compound id, TruthOperator belief, TruthOperator goal, boolean beliefProjected) {
        super(id);
        this.belief = belief;
        this.goal = goal;
        this.beliefProjected = beliefProjected;
    }

    @Override
    public float cost() {
        return 2f;
    }

    /**
     * create a Pre-Solve predicate for quick filtering in the pre tests
     */
    public Iterable<PrediTerm<Derivation>> preSolve() {
        return Collections.emptyList();

//        List<PrediTerm<Derivation>> l = $.newArrayList();
//        boolean override = this instanceof SolvePuncOverride;
//        if (override) {
//            switch (((SolvePuncOverride) this).puncOverride) {
//                case QUESTION:
//                case QUEST:
//                    l.add(NotCyclic); //since this is the provided punctuation it will need tested always
//                    //TODO: other cases: single test, etc..
//                    break;
//            }
//        } else {
//            //determined by the derivation itself so needs tested in any case
//            l.add(NotCyclicIfTaskIsQuestionOrQuest);
//        }
//        return l;
    }

    @Override
    public final boolean test(@NotNull Derivation d) {

        boolean single;
        Truth t;

        byte punc = punc(d);
        switch (punc) {
            case BELIEF:
            case GOAL:
                TruthOperator f = (punc == BELIEF) ? belief : goal;
                if (f == null)
                    return false; //there isnt a truth function for this punctuation

                single = f.single();

                if (!single) {
                    if ((beliefProjected ? d.beliefTruth : d.beliefTruthRaw) == null)
                        return false; //double premise requiring a belief, but belief is null
                }

                if (!f.allowOverlap()) {
                    float co = (single ? d.cyclic : d.overlap);
                    if (co >= 1f || d.random.nextFloat() < co) {
                        return false;
                    }
                }

                float confMin = d.confMin;

                float s = d.nar.deriverity.floatValue();
                if (s == 1.0) {
                    //truth function is single premise so set belief truth to be null to prevent any negations below:


                    if ((t = f.apply(
                            d.taskTruth, //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
                            single ? null : beliefProjected ? d.beliefTruth : d.beliefTruthRaw,
                            d.nar, confMin
                    )) == null)
                        return false;
                } else {

                    float baseConf = single ? d.premiseConfSingle : d.premiseConfDouble;

                    if ((t = f.apply(
                            d.taskTruth, //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
                            single ? null : beliefProjected ? d.beliefTruth : d.beliefTruthRaw,
                            d.nar, Param.TRUTH_EPSILON
                    )) == null)
                        return false;

                    float newEvi = Util.lerp(s, c2wSafe(baseConf, Param.HORIZON), t.evi());
                    t = new PreciseTruth(t.freq(), newEvi, false);
                }

                t = t.ditherFreqConf(d.truthResolution, confMin, 1f);
                if (t == null)
                    return false;

                break;

            case QUEST:
            case QUESTION:
                if (d.cyclic >= 1f || (d.cyclic > 0 && d.random.nextFloat() < d.cyclic))
                    return false; //HANDLED IN PRESOLVE CASES

                byte tp = d.taskPunct;
                if ((tp == QUEST) || (tp == GOAL))
                    punc = QUEST; //use QUEST in relation to GOAL or QUEST task

                single = true;
                t = null;
                break;

            default:
                throw new InvalidPunctuationException(punc);
        }

        d.concTruth = t;
        d.concPunc = punc;
        d.single = single;
        return true;
    }


    public abstract byte punc(Derivation d);

    /**
     * Created by me on 5/26/16.
     */
    public static final class SolvePuncOverride extends Solve {
        private final byte puncOverride;


        public SolvePuncOverride(Compound i, byte puncOverride, TruthOperator belief, TruthOperator desire, boolean beliefProjected) {
            super(i, belief, desire, beliefProjected);
            this.puncOverride = puncOverride;
        }


        @Override
        public byte punc(Derivation d) {
            return puncOverride;
        }

    }

    /**
     * Created by me on 5/26/16.
     */
    public static final class SolvePuncFromTask extends Solve {

        public SolvePuncFromTask(Compound i, TruthOperator belief, TruthOperator desire, boolean beliefProjected) {
            super(i, belief, desire, beliefProjected);
        }

        @Override
        public byte punc(Derivation d) {
            return d.taskPunct;
        }

    }


//    static final AbstractPred<Derivation> NotCyclic = new AbstractPred<Derivation>($.the("notCyclic")) {
//
//        @Override
//        public boolean test(Derivation d) {
//            return !d.cyclic;
//        }
//
//        @Override
//        public float cost() {
//            return 0.1f;
//        }
//    };

//    static final AbstractPred<Derivation> NotCyclicIfTaskIsQuestionOrQuest = new AbstractPred<Derivation>($.the("notCyclicIfTaskQue")) {
//
//        @Override
//        public float cost() {
//            return 0.15f;
//        }
//
//        @Override
//        public boolean test(Derivation d) {
//            if (d.cyclic) {
//                byte p = d.taskPunct;
//                if (p == QUESTION || p == QUEST)
//                    return false;
//            }
//            return true;
//        }
//
//    };
}

