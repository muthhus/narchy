package nars.exe;

import jcog.event.On;
import jcog.exe.Schedulearn;
import jcog.list.FasterList;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Causable;
import nars.control.Premise;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.NativeTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * manages low level task scheduling and execution
 *
 */
abstract public class Exec implements Executor, PriMerge {

    protected NAR nar;

    private On onClear;


    /** schedules the task for execution but makes no guarantee it will ever actually execute */
    abstract public void add(/*@NotNull*/ ITask input);

    /** an estimate or exact number of parallel processes this runs */
    abstract public int concurrency();


    abstract public Stream<ITask> stream();

    public synchronized void start(NAR nar) {
        this.nar = nar;

        assert(onClear == null);
        onClear = nar.eventClear.on((n)->clear());
    }

    public synchronized void stop() {
        if (onClear!=null) {
            onClear.off();
            onClear = null;
        }
    }

    protected synchronized void clear() {

    }

  /** visits any pending tasks */
    @Deprecated public final void forEach(Consumer<ITask> each) {
        stream().filter(Objects::nonNull).forEach(each);
    }


    /** true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to bve safe.
     */
    public boolean concurrent() {
        return concurrency() > 1;
    }

    protected void ignore(@NotNull Task t) {
        t.delete();
        nar.emotion.taskIgnored.increment();
    }

    @Override
    public void execute(Runnable r) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(r);
        } else {
            r.run();
        }
    }


    public void print(PrintStream out) {
        out.println(this);
    }

    @Override
    public float merge(Priority existing, Prioritized incoming) {
        if (existing instanceof Activate) {
            return Param.activateMerge.merge(existing, incoming);
        } else if (existing instanceof Premise) {
            ((Premise)existing).merge((Premise)incoming);
            return Param.premiseMerge.merge(existing, incoming);
        }else {
            if (existing instanceof NALTask) {
                ((NALTask)existing).causeMerge((NALTask) incoming);
            }
            return Param.taskMerge.merge(existing, incoming);
        }

    }

    public float load() {
        return 0;
    }


    final Schedulearn sched = new Schedulearn();

    public void cause(FasterList<Causable> causables) {

        List<Schedulearn.Can> can = new FasterList(causables.size());
        for (Causable c : causables)
            can.add(c.can);

        double defaultCycleTime = 1.0; //sec

        double nextCycleTime = Math.max(1, nar.exe.concurrency() - 1) * (nar.loop.isRunning() ? nar.loop.periodMS.intValue() * 0.001 : defaultCycleTime);

        sched.solve(can,
            nextCycleTime
        );

        //System.out.println(Arrays.toString(iter));

        for (int i = 0, causablesSize = causables.size(); i < causablesSize; i++) {
            Causable c = causables.get(i);
            int ii = (int) Math.ceil(c.can.iterations.value());
            if (ii > 0)
                nar.input(new InvokeCause(causables.get(i), ii));
        }
    }

    final private static class InvokeCause extends NativeTask {

        public final Causable cause;
        public final int iterations;

        private InvokeCause(Causable cause, int iterations) {
            assert (iterations > 0);
            this.cause = cause;
            this.iterations = iterations;
        }
        //TODO deadline? etc

        @Override
        public String toString() {
            return cause + ":" + iterations + "x";
        }

        @Override
        public @Nullable Iterable<? extends ITask> run(NAR n) {
            cause.run(n, iterations);
            return null;
        }
    }



}
