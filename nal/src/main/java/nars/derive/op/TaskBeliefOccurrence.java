package nars.derive.op;

import nars.Task;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * fast task/belief occurrence tests
 */
abstract public class TaskBeliefOccurrence extends AbstractPred<Derivation> {

    TaskBeliefOccurrence(@NotNull String x) {
        super(x);
    }

//    public static final PrediTerm<Derivation> beliefDTSimultaneous = new TaskBeliefOccurrence("(beliefDTSimultaneous)") {
//
//        @Override
//        public boolean test(@NotNull Derivation m) {
//            if (m.belief != null) {
//                int dt = m.belief.dt();
//                return (dt == DTERNAL) || (dt == 0);
//            }
//            return false;
//        }
//
//    };

@Override
        public float cost() {
            return 0.1f;
        }
    @Nullable
    public static final PrediTerm bothEvents = new TaskBeliefOccurrence("(bothEvents)") {

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            return b != null && !b.isEternal() && !m.task.isEternal();
        }
    };

    /** same eternality */
    @Nullable public static final PrediTerm eventsOrEternals = new TaskBeliefOccurrence("(eventsOrEternals)") {

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            if (b == null)
                return false;
            return m.task.isEternal() == b.isEternal();
        }
    };

    /** both task and belief are temporal and task precedes belief */
    @Nullable public static final PrediTerm after = new TaskBeliefOccurrence("(eventsOrEternals)") {

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            if (b == null)
                return false;
            Task t = m.task;
            return (!t.isEternal() && !b.isEternal()) && (t.start() <= b.end());
        }
    };

    /** both task and belief are eternal, or task precedes belief */
    @Nullable public static final PrediTerm afterOrEternals = new TaskBeliefOccurrence("(eventsOrEternals)") {

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            if (b == null)
                return false;
            Task t = m.task;
            boolean ete = t.isEternal();
            return (ete == b.isEternal()) && (ete || t.start() <= b.end());
        }
    };

//    /**
//     * task is before or simultaneous with belief which follows (T ... B)
//     */
//    @Nullable
//    public static final TaskBeliefOccurrence afterOrEternal = new TaskBeliefOccurrence("(afterOrEternal)") {
//
//        @Override
//        public boolean test(@NotNull Derivation m) {
//
//            /* true if belief is present and both task and belief are eternal */
//            Task b = m.belief;
//            if (b == null) return false;
//
//            long bOcc = b.start();
//            boolean bEternal = (bOcc == ETERNAL);
//
//            long tOcc = m.task.start();
//            boolean tEternal = (tOcc == ETERNAL);
//            return tEternal ?
//                    (bEternal) : //enforce lexical ordering so that the reverse isnt also computed
//                    (!bEternal && (bOcc <= tOcc));
//        }
//    };
}