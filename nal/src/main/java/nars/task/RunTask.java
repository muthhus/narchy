package nars.task;

import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** wraps a Runnable and holds a constant 1.0 priority.
 *  for system-level tasks of which the reasoner remains oblivious
 * */
public class RunTask implements ITask {

    final Runnable run;

    public RunTask(@NotNull Runnable runnable) {
        run = runnable;
    }

    @Override
    public float pri() {
        return 1;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public float setPri(float p) {
        return 1f; //does nothing
    }

    @Override
    public @Nullable Priority clonePri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Iterable<? extends ITask> run(NAR n) {
        run.run();
        return null;
    }

}
