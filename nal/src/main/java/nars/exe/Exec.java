package nars.exe;

import jcog.Util;
import jcog.event.On;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.NARLoop;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.Util.RouletteControl.*;

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


    public void cause(FasterList<Causable> causables) {

        int cc = causables.size();
        if (cc == 0)
            return;

        long dt = nar.time.sinceLast();


        /** factor to multiply the mean iteration to determine demand for the next cycle.
         *  this allows the # of iterations to continually increase.
         either:
         1) a cause will not be able to meet this demand and this value
         will remain relatively constant
         2) a cause will hit a point of diminishing returns where increasing
         the demand does not yield any more value, and the system should
         continue to tolerate this dynamic balance
         3) a limit to the # of iterations that a cause is able to supply in a cycle
         has not been determined (but is limited by the hard limit factor for safety).
         */
        float ITERATION_DEMAND_GROWTH = 2f;

        final int ITERATION_DEMAND_MAX = 64 * 1024;


//        /** set this to some cpu duty cycle fraction of the target fps */
        NARLoop l = nar.loop;
        long targetCycleTimeNS = 0;
        if (l.isRunning()) {
            //double frameTime = l.dutyTime.getMean();
            //if (frameTime > 1000*l.periodMS.intValue())
            //System.out.println("frameTime: " + n4(1000 *  frameTime) + " ms");
            targetCycleTimeNS = l.periodMS.intValue() * 1000000 * nar.exe.concurrency();
//            targetCycleTimeNS = Math.max( // ms * threads?
//                    l.periodMS.intValue() * 1000000 ,
//                    Math.round(frameTime * 1.0E9)
//            );
        } else {
        //if (targetCycleTimeNS==0) {
            //some arbitrary default target duty cycle length
            targetCycleTimeNS = 100 * 1000000 * nar.exe.concurrency();
        }

//
//        /** if each recieved exactly the same amount of time, this would be how much is allocated to each */
        @Deprecated final float targetCycleTimeNSperEach = targetCycleTimeNS / cc;

        //Benefit to cost ratio (estimate)
        //https://en.wikipedia.org/wiki/Benefit%E2%80%93cost_ratio
        //BCR = Discounted value of incremental benefits ÷ Discounted value of incremental costs
        float[] bcr = new float[cc];
        float[] granular = new float[cc];
        int[] iterLimit = new int[cc];
        float bcrTotal = 0;
        RecycledSummaryStatistics bcrStat = new RecycledSummaryStatistics();
        for (int i = 0, causablesSize = cc; i < causablesSize; i++) {
            Causable c = causables.get(i);
            float time = (float) Math.max(1, c.exeTimeNS());
            float iters = (float) Math.max(1, c.iterationsMean());
            iterLimit[i] = Math.min(ITERATION_DEMAND_MAX, Math.round((iters + 1) * ITERATION_DEMAND_GROWTH));

            float vv = Util.unitize(c.value());

            bcrStat.accept(
                    bcr[i] = vv / time
            );
            granular[i] = iters / (time / targetCycleTimeNS);
        }
//        for (int i = 0, causablesSize = cc; i < causablesSize; i++) {
//            bcr[i] = bcrStat.normalize(bcr[i]);
//        }

        float[] iter = new float[cc];
        Arrays.fill(iter, 1);



        final int[] samplesRemain = {
                3 * cc
        };

        float throttle =
                1f / samplesRemain[0];
                //(1f - (float)Math.sqrt(nar.exe.load())) / samplesRemain[0];



        Util.decideRoulette(cc, (c) -> bcr[c], nar.random(), (j) -> {

            iter[j] += granular[j] * throttle;
            boolean changedWeights = false;
            int li = iterLimit[j];
            if (iter[j] >= li) {
                iter[j] = li;
                bcr[j] = 0;
                changedWeights = true;
            }

            if (samplesRemain[0]-- <= 0) return STOP;
            else {
                return changedWeights ? WEIGHTS_CHANGED : CONTINUE;
            }
        });

        //System.out.println(Arrays.toString(iter));

        for (int i = 0, causablesSize = cc; i < causablesSize; i++) {
            int ii = (int) Math.ceil(iter[i]);
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
