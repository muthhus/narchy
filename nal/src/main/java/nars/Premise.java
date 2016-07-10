package nars;

import nars.task.Task;
import nars.task.Tasked;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the conditions used in an instance of a derivation
 */
public interface Premise extends Tasked {

    @NotNull
    @Override
    Task task();

    @NotNull
    Termed beliefTerm();

    @Nullable
    Task belief();


    /** true if both task and (non-null) belief are temporal events */
    default boolean isEvent() {
        /* TODO This part is used commonly, extract into its own precondition */
        Task b = belief();
        return (b!=null) && (!task().isEternal()) && (!b.isEternal());
    }

    @FunctionalInterface
    interface OccurrenceSolver {
        long compute(long taskOcc, long beliefOcc);
    }

    default long occurrenceTarget(@NotNull OccurrenceSolver s) {
        long tOcc = task().occurrence();
        Task b = belief();
        if (b == null) return tOcc;
        else {
            long bOcc = b.occurrence();
            return s.compute(tOcc, bOcc);

//            //if (bOcc == ETERNAL) {
//            return (tOcc != ETERNAL) ?
//                        whenBothNonEternal.compute(tOcc, bOcc) :
//                        ((bOcc != ETERNAL) ?
//                            bOcc :
//                            ETERNAL
//            );
        }
    }

}
