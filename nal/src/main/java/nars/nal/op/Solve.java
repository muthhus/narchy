package nars.nal.op;

import nars.Op;
import nars.Symbols;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.TruthOperator;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluates the truth of a premise
 */
abstract public class Solve extends AtomicBoolCondition {

    private final transient String id;

    public final Derive derive;

    public final TruthOperator belief;
    public final TruthOperator desire;

    public Solve(String id, Derive derive, TruthOperator belief, TruthOperator desire) {
        super();
        this.id = id;
        this.derive = derive;
        this.belief = belief;
        this.desire = desire;
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

    final boolean measure(@NotNull PremiseEval m, char punct) {
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



        if (!tf.allowOverlap()) {

            //single premise
            if (m.cyclic)
                return false;

            if (!tf.single()) {
                //double premise
                if (m.overlap)
                    return false;
            }
        }



//        float minConf = m.confMin;


//        Truth truth = tf.apply(
//                m.taskTruth,
//                m.beliefTruth,
//                m.nar,
//                minConf
//        );
//
//
//        //pre-filter insufficient confidence level
//
//        if (truth != null) {
//
////            if (Global.DEBUG) {
////                if (!tf.single() && belief == null) {
////                    throw new RuntimeException("null belief but non-single truth function");
////                }
////            }
//
//            if ( truth.conf() > minConf) {
//                m.truth.set(truth);
//                return true;
//            }
//            //use this to find truth functions which do not utilize minConf before allocating a result Truth instance
//            /*else {
//                throw new RuntimeException(this + " did not filter minConf");
//            }*/
//        }

        return true;
    }


}

