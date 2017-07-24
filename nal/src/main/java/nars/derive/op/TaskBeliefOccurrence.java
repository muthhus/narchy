package nars.derive.op;

import nars.Task;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * fast task/belief occurrence tests
 */
abstract public class TaskBeliefOccurrence extends AbstractPred<Derivation> {

    public TaskBeliefOccurrence(@NotNull String x) {
        super(x);
    }

    public static final PrediTerm<Derivation> beliefDTSimultaneous = new TaskBeliefOccurrence("(beliefDTSimultaneous)") {

        @Override
        public boolean test(@NotNull Derivation m) {
            if (m.belief != null) {
                int dt = m.belief.dt();
                return (dt == DTERNAL) || (dt == 0);
            }
            return false;
        }

    };


    @Nullable
    public static final TaskBeliefOccurrence bothEvents = new TaskBeliefOccurrence("(bothEvents)") {

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            return b != null && !b.isEternal() && !m.task.isEternal();
        }
    };

    @Nullable
    public static final TaskBeliefOccurrence eventsOrEternals = new TaskBeliefOccurrence("(eventsOrEternals)") {

        @Override
        public boolean test(@NotNull Derivation m) {
            Task b = m.belief;
            if (b == null)
                return false;
            return m.task.isEternal() == b.isEternal();
        }
    };

    /**
     * task is before or simultaneous with belief which follows (T ... B)
     */
    @Nullable
    public static final TaskBeliefOccurrence afterOrEternal = new TaskBeliefOccurrence("(afterOrEternal)") {

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
}