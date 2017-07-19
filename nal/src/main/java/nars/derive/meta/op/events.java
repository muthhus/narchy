package nars.derive.meta.op;

import nars.Task;
import nars.control.premise.Derivation;
import nars.derive.meta.AtomicPred;
import nars.derive.meta.PrediTerm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * True if the premise task and belief are both non-eternal events
 */
abstract public class events extends AtomicPred<Derivation> {

//    /** task is before or simultaneous with belief which follows (T ... B) */
//    public static final events nonEternal = new events() {
//
//        @Override
//        public String toString() {
//            return "nonEternal";
//        }
//
//        @Override
//        public boolean test(@NotNull Derivation m) {
//            return deltaOcc(m.task, m.belief) !=ETERNAL; /* && d!=DTERNAL which is negative */
//        }
//
//    };

//    /** task is before or simultaneous with belief which follows (T ... B) */
//    public static final events after = new events() {
//
//        @Override
//        public String toString() {
//            return "after";
//        }
//
//        @Override
//        public boolean test(@NotNull Derivation m) {
//            long d = deltaOcc(m.task, m.belief);
//            return d<=0 && d!=ETERNAL; /* && d!=DTERNAL which is negative */
//        }
//
//    };
//    /** true if both are non-eternal and task is after or simultaneous with belief  */
//    @Nullable
//    public static final events before = new events() {
//
//        @Override
//        public String toString() {
//            return "before";
//        }
//
//        @Override
//        public boolean run(@NotNull Derivation m) {
//            long d = deltaOcc(m.task, m.belief);
//            return d>=0 /* && d!=ETERNAL which is negative */;
//        }
//
//    };

//    /** order doesnt matter, just that they are both temporal */
//    @Nullable public static final events bothTemporal = new events() {
//
//        @Override
//        public String toString() {
//            return "bothTemporal";
//        }
//
//        @Override
//        public boolean run(@NotNull Derivation m) {
//            int d = deltaOcc(m.task, m.belief);
//            return d!=DTERNAL;
//        }
//
//    };

//    public static final BoolCondition<Derivation> taskNotDTernal = new events() {
//
//        @Override
//        public String toString() {
//            return "taskNotDTernal";
//        }
//
//        @Override
//        public boolean run(@NotNull Derivation m) {
//            int dt = m.task.dt();
//            return (dt != DTERNAL);
//        }
//
//    };

    public static final PrediTerm<Derivation> beliefDTSimultaneous = new events() {

        @Override
        public String toString() {
            return "beliefDTSimultaneous";
        }

        @Override
        public boolean test(@NotNull Derivation m) {
            if (m.belief!=null) {
                int dt = m.belief.dt();
                return (dt == DTERNAL) || (dt == 0);
            }
            return false;
        }

    };

//    /** true if the belief term is in the earliest position of a conjunction.
//     * for parallel and eternal, automatically true.
//     *  */
//    public static BoolCondition<Derivation> beliefTermEarliest = new events() {
//        @Override
//        public @NotNull String toString() {
//            return "beliefTermEarliest";
//        }
//
//        @Override
//        public boolean run(Derivation p, int now) {
//            Compound taskTerm = p.taskTerm;
//            int dt = taskTerm.dt();
//            return (dt == DTERNAL || dt == 0 || taskTerm.subtermTime( p.beliefTerm ) == 0);
//        }
//    };


//    public static final BoolCondition<Derivation> taskConjDecomposable = new AtomicBoolCondition<Derivation>() {
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

//    public static long deltaOcc(@NotNull Task a, @Nullable Task b) {
//
//        if (b == null)
//            return ETERNAL;
//
//        long tOcc = a.start();
//        if (tOcc == ETERNAL)
//            return ETERNAL;
//
//        long bOcc = b.start();
//        if (bOcc == ETERNAL)
//            return ETERNAL;
//
//        return (tOcc - bOcc);
//    }

//    /** both task and belief must be non-null and eternal */
//    @Nullable
//    public static final events eternal = new events() {
//
//        @Override
//        public String toString() {
//            return "eternal";
//        }
//
//        @Override
//        public boolean run(@NotNull Derivation m) {
//            Task b = m.belief;
//            if (b == null) return false;
//            return m.task.start() == ETERNAL && b.start() == ETERNAL;
//        }
//    };




    @Nullable
    public static final events bothEvents = new events() {

        @Override
        public String toString() {
            return "bothEvents";
        }

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            return b != null && !b.isEternal() && !m.task.isEternal();
        }
    };

    @Nullable
    public static final events eventsOrEternals = new events() {

        @Override
        public String toString() {
            return "eventsOrEternals";
        }

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            if (b==null)
                return false;
            return m.task.isEternal() == b.isEternal();
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
        public boolean test(@NotNull Derivation m) {

            /* true if belief is present and both task and belief are eternal */
            Task b = m.belief;
            if (b == null) return false;

            long bOcc = b.start();
            boolean bEternal = (bOcc == ETERNAL);

            long tOcc = m.task.start();
            boolean tEternal = (tOcc == ETERNAL);
            return tEternal ?
                    (bEternal) : //enforce lexical ordering so that the reverse isnt also computed
                    (!bEternal && (bOcc <= tOcc));
        }
    };
//    public static final @Nullable BoolCondition<Derivation> ifTermLinkBefore = new events() {
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
//    @Nullable
//    public static final events lexicalIfEternal = new events() {
//
//        @Override
//        public String toString() {
//            return "lexicalIfEternal";
//        }
//
//        @Override
//        public boolean run(@NotNull Derivation m) {
//
//            /* true if belief is present and both task and belief are eternal */
//            Task b = m.belief;
//            if (b == null) return false;
//
//            int tdt = m.task.dt();
//            int bdt = b.dt();
//            if ((tdt == DTERNAL) && (bdt == DTERNAL)) {
//                return m.task.term().compareTo(b.term()) < 0; //lexical diode
//            } else if ((tdt != DTERNAL) && (bdt != DTERNAL)) {
//                return tdt > bdt;
//            } else {
//                return true;
//            }
////            long bOcc = b.start();
////            boolean bEternal = (bOcc == ETERNAL);
////
////            long tOcc = m.task.start();
////            boolean tEternal = (tOcc == ETERNAL);
////            return tEternal ? true :
////                    (bEternal || m.task.term().compareTo(b.term()) < 0); //enforce lexical ordering so that the reverse isnt also computed
//
//        }
//    };
