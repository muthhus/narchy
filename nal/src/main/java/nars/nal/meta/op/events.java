package nars.nal.meta.op;

import nars.nal.ConceptProcess;
import nars.nal.Tense;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.BoolCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;

/**
 * True if the premise task and belief are both non-eternal events
 */
abstract public class events extends AtomicBoolCondition {

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

    public static boolean beliefBeforeOrDuringTask(@NotNull ConceptProcess p) {
        Task b = p.belief();
        if (b == null) return false;
        long tOcc = p.task().occurrence();
        long bOcc = b.occurrence();
        return !Tense.isEternal(bOcc) &&
                !Tense.isEternal(tOcc) &&
                ((bOcc - tOcc) >= 0);
    }



    /** task is before or simultaneous with belief which follows (T ... B) */
    @Nullable
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
//    public static final @Nullable BoolCondition ifTermLinkBefore = new events() {
//
//        @Override
//        public String toString() {
//            return "ifBeliefIsBefore";
//        }
//    };

    public static final @Nullable BoolCondition ifTermLinkBefore = new IfTermLinkBefore();
    public static final @Nullable BoolCondition ifBeliefBefore = new IfTermLinkBefore() {
        @Override
        public String toString() {
            return "ifBeliefIsBefore";
        }
        @Override
        public boolean requireBelief() {
            return true;
        }

    };

    private static class IfTermLinkBefore extends events {
        @Override
        public String toString() {
            return "ifTermLinkBefore";
        }

        public boolean requireBelief() {
            return false;
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval m) {

            Task belief = m.premise.belief();
            if (belief == null && requireBelief())
                return false;

            Task task = m.premise.task();
            Compound tt = task.term();
            int ttdt = tt.dt();


            if ((belief!=null) && (belief.occurrence()!=ETERNAL) && (task.occurrence()!=ETERNAL)) {
                //only allow a belief if it occurred before or during the task's specified occurrence
                if (belief.occurrence() > task.occurrence())
                    return false;
            }

            if ((ttdt == DTERNAL) || (ttdt == 0)) {
                return true;
            } else {

                final int targetMatch;  //must match term
                if (ttdt < 0) { //time reversed
                    targetMatch = 1;
                } else /*if (ttdt > 0) */{ //time forward
                    targetMatch = 0;
                }
                return tt.term(targetMatch).equals(m.beliefTerm);
            }
        }

    }

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



}
