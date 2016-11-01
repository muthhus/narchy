package nars.nal.meta.op;

import nars.Task;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.BoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

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
        public boolean run(@NotNull PremiseEval m, int now) {
            int d = deltaOcc(m.task, m.belief);
            return d<=0 && d!=DTERNAL; /* && d!=DTERNAL which is negative */
        }

    };
    /** true if both are non-eternal and task is after or simultaneous with belief  */
    @Nullable
    public static final events before = new events() {

        @Override
        public String toString() {
            return "before";
        }

        @Override
        public boolean run(@NotNull PremiseEval m, int now) {
            int d = deltaOcc(m.task, m.belief);
            return d>=0 /* && d!=DTERNAL which is negative */;
        }

    };

    /** order doesnt matter, just that they are both temporal */
    @Nullable public static final events bothTemporal = new events() {

        @Override
        public String toString() {
            return "bothTemporal";
        }

        @Override
        public boolean run(@NotNull PremiseEval m, int now) {
            int d = deltaOcc(m.task, m.belief);
            return d!=DTERNAL;
        }

    };

    public static final BoolCondition taskNotDTernal = new events() {

        @Override
        public String toString() {
            return "taskNotDTernal";
        }

        @Override
        public boolean run(@NotNull PremiseEval m, int now) {
            int dt = m.task.dt();
            return (dt != DTERNAL);
        }

    };


//    public static final BoolCondition taskConjDecomposable = new AtomicBoolCondition() {
//        @Override
//        public @NotNull String toString() {
//            return "taskConjDecomposable";
//        }
//
//        @Override
//        public boolean booleanValueOf(PremiseEval p) {
//            Task t = p.task;
//
//            // rejects any task term with dt==DTERNAL and if task and belief are both eternal */
//            /*if (t.dt()!=DTERNAL) {
//                if (!t.isEternal())
//                    return true;
//                Task b = p.belief;
//                return (b!=null && !p.belief.isEternal());
//            } else {
//                return true; //dternal
//            }*/
//            return true;
//        }
//    };

    public static int deltaOcc(@NotNull Task a, @Nullable Task b) {

        if (b == null)
            return DTERNAL;

        long tOcc = a.occurrence();
        if (tOcc == ETERNAL)
            return DTERNAL;

        long bOcc = b.occurrence();
        if (bOcc == ETERNAL)
            return DTERNAL;

        return (int)(tOcc - bOcc);
    }

    /** both task and belief must be non-null and eternal */
    @Nullable
    public static final events eternal = new events() {

        @Override
        public String toString() {
            return "eternal";
        }

        @Override
        public boolean run(@NotNull PremiseEval m, int now) {
            Task b = m.belief;
            if (b == null) return false;
            return m.task.occurrence() == ETERNAL && b.occurrence() == ETERNAL;
        }
    };


    /** task is before or simultaneous with belief which follows (T ... B) */
    @Nullable
    public static final events afterOrEternal = new events() {

        @Override
        public String toString() {
            return "afterOrEternal";
        }

        @Override
        public boolean run(@NotNull PremiseEval m, int now) {

            /* true if belief is present and both task and belief are eternal */
            Task b = m.belief;
            if (b == null) return false;

            long bOcc = b.occurrence();
            boolean bEternal = (bOcc == ETERNAL);

            long tOcc = m.task.occurrence();
            boolean tEternal = (tOcc == ETERNAL);
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
