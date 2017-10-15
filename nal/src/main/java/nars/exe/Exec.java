package nars.exe;

import jcog.event.On;
import jcog.exe.Can;
import jcog.exe.Schedulearn;
import jcog.list.FasterList;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.NARLoop;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Premise;
import nars.task.ITask;
import nars.task.NALTask;
import jcog.constraint.continuous.exceptions.InternalSolverError;
import nars.task.NativeTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.Util.sqr;

/**
 *
 * manages low level task scheduling and execution
 *
 */
abstract public class Exec implements Executor, PriMerge {

    private static final Logger logger = LoggerFactory.getLogger(Exec.class);

    protected NAR nar;

    private On onClear;


    /** schedules the task for execution but makes no guarantee it will ever actually execute */
    abstract public void add(/*@NotNull*/ ITask input);


    protected void execute(ITask x) {

        try {

            Iterable<? extends ITask> y = x.run(nar);
            if (y != null)
                y.forEach(this::add);

        } catch (Throwable e) {
            if (Param.DEBUG) {
                throw e;
            } else {
                logger.error("{} {}", x, (Param.DEBUG) ? e : e.getMessage());
                x.delete();
            }
        }


    }

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
                ((NALTask)existing).causeMerge((Task) incoming);
            }
            return Param.taskMerge.merge(existing, incoming);
        }

    }

    public float load() {
        return 0;
    }


    final Schedulearn sched = new Schedulearn();

    public void cause(FasterList<Can> can) {

        double defaultCycleTime = 1.0; //sec


        NARLoop loop = nar.loop;

        double nextCycleTime = Math.max(1, concurrency() - 1) * (
                loop.isRunning() ? loop.periodMS.intValue() * 0.001 : defaultCycleTime
        );

        float throttle = loop.throttle.floatValue();
        double dutyCycleTime = nextCycleTime * throttle * (1f - sqr(nar.exe.load()));
        double sleepTime = nextCycleTime * (1f - throttle);

        if (dutyCycleTime > 0) {
            try {
                sched.solve(can, dutyCycleTime);

                //sched.estimatedTimeTotal(can);
            } catch (InternalSolverError e) {
                logger.error("{} {}", can, e);
            }
        }

        final double MIN_SLEEP_TIME = 0.001f; //1 ms
        final int sleepGranularity = 2;
        int divisor = sleepGranularity * concurrency();
        double sleepEach = sleepTime / divisor;
        if (sleepEach >= MIN_SLEEP_TIME) {
            int msToSleep = (int)Math.ceil(sleepTime*1000);
            nar.exe.add(new NativeTask.SleepTask(msToSleep, divisor));
        }

    }


}
