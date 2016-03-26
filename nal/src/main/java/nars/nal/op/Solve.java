package nars.nal.op;

import nars.Op;
import nars.Premise;
import nars.Symbols;
import nars.concept.ConceptProcess;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.TruthOperator;
import nars.task.Task;
import nars.truth.BeliefFunction;
import nars.truth.DesireFunction;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Evaluates the truth of a premise
 */
abstract public class Solve extends AtomicBooleanCondition<PremiseEval> {

    private final transient String id;

    private final Derive derive;

    public Solve(String id, Derive derive) {
        super();
        this.id = id;
        this.derive = derive;
    }




    //
//        try {
//            MethodHandles.Lookup l = MethodHandles.publicLookup();
//
//            this.method = puncOverride != 0 ? Binder.from(boolean.class, PremiseMatch.class)
//                    .append(puncOverride)
//                    .append(TruthOperator.class, belief)
//                    .append(TruthOperator.class, desire)
//                    .invokeStatic(l, Solve.class, "measureTruthOverride") : Binder.from(boolean.class, PremiseMatch.class)
//                    .append(TruthOperator.class, belief)
//                    .append(TruthOperator.class, desire)
//                    .invokeStatic(l, Solve.class, "measureTruthInherit");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

//    @Override
//    public String toJavaConditionString() {
//        String s = "";
//        String solver = Solve.class.getName();
//        s += solver + ".measureTruth(m, ";
//        s += puncOverride == 0 ? "p.getTask().getPunctuation()" : "'" + puncOverride + "'";
//        s += ", nars.truth.BeliefFunction." + belief + ", ";
//        s += desire != null ? "nars.truth.DesireFunction." + desire : "null";
//        s += ")";
//        return s;
//    }

//    @Override
//    public final boolean booleanValueOf(PremiseMatch m) {
//        boolean r = false;
//        try {
//            r=(boolean)method.invokeExact(m);
//        } catch (Throwable throwable) {
//            //throw new RuntimeException(throwable);
//            // throwable.printStackTrace();  // return false;
//        }
//        return r;
//    }


//    /** inherits punctuation from task */
//    public static class SolveInherit extends Solve {
//
//        @Override public boolean booleanValueOf(PremiseMatch m) {
//            char punct = m.premise.getTask().getPunctuation();
//            return measureTruthOverride(m, punct, belief, desire);
//        }
//    }
//
//    /** overrides punctuation */
//    public static class SolveOverride extends Solve {
//
//    }

//    public static boolean measureTruthInherit(PremiseMatch m, TruthOperator belief, TruthOperator desire) {
//        char punct = m.premise.getTask().getPunctuation();
//        return measureTruthOverride(m, punct, belief, desire);
//    }

    static boolean measure(@NotNull PremiseEval m, char punct, TruthOperator belief, TruthOperator desire) {
        boolean r;
        switch (punct) {
            case Symbols.BELIEF:
            case Symbols.GOAL:
                TruthOperator tf = (punct == Symbols.BELIEF) ? belief : desire;
                r = (tf != null) && (Solve.measure(tf, m));
                break;
            case Symbols.QUESTION:
            case Symbols.QUEST:
                m.truth.set(null);
                r = true; //a truth function is not involved, so succeed
                break;
            default:
                throw new Op.InvalidPunctuationException(punct);
        }

        if (r)
            m.punct.set(punct);

        return r;
    }

    private static boolean measure(@NotNull TruthOperator tf, @NotNull PremiseEval m) {

        ConceptProcess p = m.premise;

        if (!tf.allowOverlap() && p.cyclic())
            return false;

        Premise premise = p;

        @Nullable Task belief = premise.belief();

        float minConf = m.getMinConfidence();

        Truth truth = tf.apply(
                premise.task().truth(),
                belief != null ? belief.truth() : null,
                premise.nar(),
                minConf
        );

        //pre-filter insufficient confidence level

        if (truth != null) {
            if ( truth.conf() > minConf) {
                m.truth.set(truth);
                return true;
            }
            //use this to find truth functions which do not utilize minConf before allocating a result Truth instance
            /*else {
                throw new RuntimeException(this + " did not filter minConf");
            }*/
        }

        return false;
    }


    public Derive getDerive() {
        return derive;
    }


    public static final class SolvePuncFromTask extends Solve {
        private final BeliefFunction belief;
        private final DesireFunction desire;

        public SolvePuncFromTask(String i, Derive der, BeliefFunction belief, DesireFunction desire) {
            super(i, der);
            this.belief = belief;
            this.desire = desire;
        }

        @Override public boolean booleanValueOf(@NotNull PremiseEval m) {
            return measure(m,
                    m.punct.get(),
                    belief, desire);
        }
    }

    public static final class SolvePuncOverride extends Solve {
        private final char puncOverride;
        private final BeliefFunction belief;
        private final DesireFunction desire;

        public SolvePuncOverride(String i, Derive der, char puncOverride, BeliefFunction belief, DesireFunction desire) {
            super(i, der);
            this.puncOverride = puncOverride;
            this.belief = belief;
            this.desire = desire;
        }

        @Override public boolean booleanValueOf(@NotNull PremiseEval m) {
            return measure(m, puncOverride, belief, desire);
        }
    }


}

