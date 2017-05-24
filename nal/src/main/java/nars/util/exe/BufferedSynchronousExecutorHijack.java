package nars.util.exe;

import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.FloatParam;
import jcog.pri.PForget;
import jcog.pri.Pri;
import nars.$;
import nars.NAR;
import nars.task.ITask;
import nars.task.NALTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.Op.COMMAND;

/**
 * Buffers all executions between each cycle in order to remove duplicates
 */
public class BufferedSynchronousExecutorHijack extends SynchronousExecutor {


    /**
     * if < 0, executes them all. 0 pauses, and finite value > 0 will cause them to be sorted first if the value exceeds the limit
     * interpreted as its integer value, although currently it is FloatParam
     */
    public final FloatParam exePerCycleMax = new FloatParam(-1);

    /**
     * temporary collection of tasks to remove after sampling
     */
    private final List<ITask> toRemove = $.newArrayList();

    /**
     * amount of priority to subtract from each processed task (re-calculated each cycle according to bag pressure)
     */
    private float forgetEachPri;

    /**
     * active tasks
     */
    public final PriorityHijackBag<ITask, ITask> active = new PriorityHijackBag<>(4) {
        @Override
        protected final Consumer<ITask> forget(float rate) {
            return null;
            //return new PForget(rate);
        }


        @NotNull
        @Override
        public final ITask key(ITask value) {
            return value;
        }
    };

    public BufferedSynchronousExecutorHijack(int capacity) {
        super();
        active.capacity(capacity);
    }

    public BufferedSynchronousExecutorHijack(int capacity, float executedPerCycle) {
        this(capacity);
        exePerCycleMax.setValue(Math.ceil(capacity * executedPerCycle));
    }

    @Override
    public void cycle(@NotNull NAR nar) {
        //flush();

        super.cycle(nar);

        flush();
    }

    @Override
    public void stop() {
        flush();
        super.stop();
    }


    @Override
    public void start(NAR nar) {
        super.start(nar);
        flush();
    }

//    @Override
//    public void stop() {
//        flush();
//        super.stop();
//    }

    AtomicBoolean busy = new AtomicBoolean(false);


    private void flush() {
        if (!busy.compareAndSet(false, true))
            return;

        try {
            int ps = active.size();
            if (ps == 0)
                return;

            boolean t = this.trace;
            if (t)
                active.print();

            int toExe = exePerCycleMax.intValue();
            if (toExe < 0)
                toExe = active.capacity();


//            toExe = Math.min(ps, toExe);

            float eFrac = ((float)toExe) / ps;
            float pAvg = (PForget.DEFAULT_TEMP) * active.depressurize(eFrac) / toExe;
            this.forgetEachPri = pAvg > Pri.EPSILON ? pAvg : 0;

            active.sample(toExe, this::actuallyRun);

            if (!toRemove.isEmpty()) {
                toRemove.forEach(active::remove);
                toRemove.clear();
            }

//            } else {
//                //sort
//                if (sorted == null || sorted.capacity() != (toExe + 1)) {
//                    sorted = new SortedArray<ITask>(new ITask[toExe + 1]);
//                }
//                pending.sample(pending.capacity(), s -> {
//                    sorted.add(s, Prioritized::oneMinusPri);
//                    if (sorted.size() > toExe)
//                        sorted.removeLast();
//                });
//                assert (sorted.size() == toExe);
//                sorted.forEach(this::actuallyRun);
//            }


        } finally {
            busy.set(false);
        }
    }

    private void actuallyRun(ITask x) {
        try {
            //super.run(x);

            ITask[] next = x.run(nar);
            if (next != null)
                for (ITask y : next)
                    if (y == null || !run(y))
                        break;


            if (forgetEachPri > 0 && !((x instanceof NALTask) && (!x.isInput())))
                x.priSub(forgetEachPri);
        } catch (Throwable e) {
            NAR.logger.error("{} {}", x, e.getMessage());
            toRemove.add(x); //TODO add to a 'bad' bag?
        }
    }


    @Override
    public boolean run(@NotNull ITask input) {
        if (input.punc() == COMMAND) {
            actuallyRun(input); //commands executed immediately
            return true;
        } else {
            return active.put(input) != null;
        }
    }

}
