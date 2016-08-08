package nars.nal.meta.op;

import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.BoolCondition;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
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
            return beliefBeforeOrDuringTask(m.task, m.belief);
        }

    };

    /** true if belief is before or during task */
    public static final events before = new events() {

        @Override
        public String toString() {
            return "before";
        }

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval m) {
            @Nullable Task b = m.belief;
            return b != null && beliefBeforeOrDuringTask(b, m.task);
        }

    };


    public static final BoolCondition taskConjDecomposable = new AtomicBoolCondition() {
        @Override
        public @NotNull String toString() {
            return "taskConjDecomposable";
        }

        @Override
        public boolean booleanValueOf(PremiseEval p) {
            Task t = p.task;

            // rejects any task term with dt==DTERNAL and if task and belief are both eternal */
            /*if (t.dt()!=DTERNAL) {
                if (!t.isEternal())
                    return true;
                Task b = p.belief;
                return (b!=null && !p.belief.isEternal());
            } else {
                return true; //dternal
            }*/
            return true;
        }
    };

    public static boolean beliefBeforeOrDuringTask(@NotNull Task a, @Nullable Task b) {

        if (b == null)
            return false;

        long tOcc = a.occurrence();
        if (tOcc == ETERNAL)
            return false;

        long bOcc = b.occurrence();
        if (bOcc == ETERNAL)
            return false;

        return ((bOcc - tOcc) >= 0);
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

            /* true if belief is present and both task and belief are eternal */
            Task b = m.belief;
            if (b == null) return false;

            long tOcc = m.task.occurrence();
            long bOcc = b.occurrence();
            boolean tEternal = (tOcc == ETERNAL);
            boolean bEternal = (bOcc == ETERNAL);
            return tEternal ? bEternal : (!bEternal && (bOcc <= tOcc));
        }
    };
//    public static final @Nullable BoolCondition ifTermLinkBefore = new events() {
//
//        @Override
//        public String toString() {
//            return "ifBeliefIsBefore";
//        }
//    };

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
