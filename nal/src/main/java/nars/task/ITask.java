package nars.task;

import jcog.pri.Priority;
import nars.NAR;
import nars.concept.Concept;
import nars.task.util.InvalidTaskException;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * generic abstract task used for commands and other processes
 */
public interface ITask extends Priority {

    default byte punc() {
        return 0;
    }

    @Nullable ITask[] run(@NotNull NAR n);

    default Priority clone() {
        throw new UnsupportedOperationException();
    }

    default boolean isInput() {
        return false;
    }

    /** id key */
    default Object key() {
        return this;
    }

    default void merge(ITask incoming) {
        priAdd(incoming.priSafe(0));
    }

    @Override
    @NotNull
    default Priority priority() {
        throw new UnsupportedOperationException("impl in subclasses");
    }
}
