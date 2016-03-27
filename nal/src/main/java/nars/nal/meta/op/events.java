package nars.nal.meta.op;

import nars.concept.ConceptProcess;
import nars.nal.Tense;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

import static nars.nal.Tense.ETERNAL;

/**
 * True if the premise task and belief are both non-eternal events
 */
abstract public class events extends AtomicBooleanCondition<PremiseEval> {

    /** task is before or simultaneous with belief which follows (T ... B) */
    public static final events after = new events() {

        @Override
        public String toString() {
            return "after";
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval m) {
            return beliefBeforeOrDuringTask(m.premise);
        }

    };

    /** task is before or simultaneous with belief which follows (T ... B) */
    public static final events afterOrEternal = new events() {

        @Override
        public String toString() {
            return "afterOrEternal";
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval m) {
            ConceptProcess p = m.premise;

            /* true if belief is present and both task and belief are eternal */
            Task b = p.belief();
            if (b == null) return false;

            long tOcc = p.task().occurrence();
            long bOcc = b.occurrence();
            boolean tEternal = (tOcc == ETERNAL);
            boolean bEternal = (bOcc == ETERNAL);
            if (tEternal) {
                return bEternal;
            } else {
                return (!bEternal && bOcc <= tOcc);
            }
        }
    };

//    /** ITERNAL or 0, used in combination with a Temporalize that uses the same dt as the task */
//    public static final events dtBeliefSimultaneous = new events() {
//        @Override
//        public boolean booleanValueOf(PremiseEval m) {
//            //Task belief = m.premise.belief();
//
//            /*if (belief == null) {
//                return (m.premise.task().term().dt() == ITERNAL);
//            } else {*/
//                return true;
//            //}
//
//            //int tdt = belief.term().dt();
//            //return (tdt == Tense.ITERNAL) || (tdt == 0);
//        }
//
//        @Override
//        public String toString() {
//            return "dtBeliefSimultaneous";
//        }
//    };

    public static boolean beliefBeforeOrDuringTask(@NotNull ConceptProcess p) {
        Task b = p.belief();
        if (b == null) return false;
        long tOcc = p.task().occurrence();
        long bOcc = b.occurrence();
        return !Tense.isEternal(bOcc) &&
                !Tense.isEternal(tOcc) &&
                ((bOcc - tOcc) >= 0);
    }



}
