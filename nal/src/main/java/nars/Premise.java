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

    @Nullable
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


}
