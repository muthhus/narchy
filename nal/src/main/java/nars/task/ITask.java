package nars.task;

import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * generic abstract task used for commands and other processes
 */
public interface ITask extends Priority {

    default byte punc() {
        return 0;
    }

    /** the first null in the returned array should break the iteration because it means its the end of the list (all following it should also be null) */
    @Nullable ITask[] run(@NotNull NAR n);

    default Priority clone() {
        throw new UnsupportedOperationException();
    }

    default boolean isInput() {
        return false;
    }

    default ITask merge(ITask incoming) {
        priAdd(incoming.priSafe(0));
        return this;
    }

}
