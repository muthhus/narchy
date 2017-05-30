package nars.task;

import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * generic abstract task used for commands and other processes
 */
public interface ITask extends Priority {

    /** note: the first null in the returned array will break the iteration because it means its the end of the list (all following it should also be null) */
    @Nullable ITask[] run(@NotNull NAR n);

    /** special signal a task can return to signal it should be deleted after execution */
    ITask[] DeleteMe = new ITask[0];

    /** special signal a task can return to signal it should be quietly removed from the bag (and not deleted or forgotten) */
    ITask[] Disappear = new ITask[0];


    default byte punc() {
        return 0;
    }


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

    /** fluent form of setPri which returns this class */
    default ITask pri(float p) {
        setPri(p);
        return this;
    }
}
