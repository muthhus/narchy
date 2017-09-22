package nars.task;

import com.google.common.primitives.Longs;
import jcog.pri.Priority;
import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * for queued/scheduled native tasks
 * the reasoner remains oblivious of these.
 * but it holds a constant 1.0 priority.
 */
public abstract class NativeTask implements ITask {

    @Override
    public float pri() {
        return 1;
    }

    @Override
    abstract public String toString();

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
    public @Nullable
    abstract Iterable<? extends ITask> run(NAR n);

    /**
     * wraps a Runnable
     */
    public static class RunTask extends NativeTask {

        final Runnable run;

        public RunTask(@NotNull Runnable runnable) {
            run = runnable;
        }

        @Override
        public String toString() {
            return run.toString();
        }

        @Override
        public @Nullable Iterable<? extends ITask> run(NAR n) {
            run.run();
            return null;
        }

    }

    public static final class SchedTask extends NativeTask implements Comparable<SchedTask> {

        public final long when;
        public final Object then;

        public SchedTask(long whenOrAfter, Consumer<NAR> then) {
            this.when = whenOrAfter;
            this.then = then;
        }

        public SchedTask(long whenOrAfter, Runnable then) {
            this.when = whenOrAfter;
            this.then = then;
        }

        @Override
        public String toString() {
            return "@" + when + ':' + then.toString();
        }

        @Override
        public final @Nullable Iterable<? extends ITask> run(NAR n) {
            if (then instanceof Runnable)
                ((Runnable) then).run();
            else
                ((Consumer) then).accept(n);
            return null;
        }

        @Override
        public int compareTo(@NotNull NativeTask.SchedTask b) {
            if (this == b)
                return 0;

            int t = Longs.compare(when, b.when);
            if (t != 0) {
                return t;
            }

            Object aa = then;
            Object bb = b.then;
            if (aa == bb) return 0;
            //as a last resort, compare their system ID
            return Integer.compare(System.identityHashCode(aa), System.identityHashCode(bb)); //maintains uniqueness in case they occupy the same time
        }
    }

    /**
     * wraps a Runnable
     */
    public static class NARTask extends NativeTask {

        final Consumer run;

        public NARTask(@NotNull Consumer<NAR> runnable) {
            run = runnable;
        }

        @Override
        public String toString() {
            return run.toString();
        }

        @Override
        public @Nullable Iterable<? extends ITask> run(NAR x) {
            run.accept(x);
            return null;
        }

    }

}
