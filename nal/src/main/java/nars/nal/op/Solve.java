package nars.nal.op;

import nars.Op;
import nars.Symbols;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.truth.func.TruthOperator;
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

    @NotNull
    @Override
    public String toString() {
        return id;
    }

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
            //if (m.cyclic)
                //return false;

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

