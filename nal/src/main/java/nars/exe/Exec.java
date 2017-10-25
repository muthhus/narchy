package nars.exe;

import jcog.constraint.continuous.exceptions.InternalSolverError;
import jcog.event.On;
import jcog.exe.Can;
import jcog.exe.Schedulearn;
import nars.NAR;
import nars.NARLoop;
import nars.Param;
import nars.concept.Concept;
import nars.control.Activate;
import nars.task.ITask;
import nars.task.NativeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.Util.sqr;

/**
 *
 * manages low level task scheduling and execution
 *
 */
abstract public class Exec implements Executor {

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
                logger.error("{} {}", x, e); //(Param.DEBUG) ? e : e.getMessage());
                x.delete();
            }
        }


    }

    abstract public void fire(int n, Predicate<Activate> each);

    /** an estimate or exact number of parallel processes this runs */
    abstract public int concurrency();


    abstract public Stream<Activate> active();

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

    abstract void clear();

    /** true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to bve safe.
     */
    public boolean concurrent() {
        return concurrency() > 1;
    }


    @Override
    public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
        }
    }

    public void print(PrintStream out) {
        out.println(this);
    }


    public float load() {
        return 0;
    }


    final Schedulearn sched = new Schedulearn();

    /** allocates what can be done */
    public void cycle(List<Can> can) {



        NARLoop loop = nar.loop;

        double nextCycleTime = Math.max(1, concurrency() - 1) * (
                loop.isRunning() ? loop.periodMS.intValue() * 0.001 : Param.SynchronousExecution_Max_CycleTime
        );

        float throttle = loop.throttle.floatValue();
        double dutyCycleTime = nextCycleTime * throttle * (1f - sqr(nar.exe.load()));

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
        double sleepTime = nextCycleTime * (1f - throttle);
        double sleepEach = sleepTime / divisor;
        if (sleepEach >= MIN_SLEEP_TIME) {
            int msToSleep = (int)Math.ceil(sleepTime*1000);
            nar.exe.add(new NativeTask.SleepTask(msToSleep, divisor));
        }

    }


    abstract public void activate(Concept c, float activationApplied);

}
