package nars.util.exe;

import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.FloatParam;
import jcog.list.FasterList;
import jcog.pri.Pri;
import nars.NAR;
import nars.task.ITask;
import nars.task.NALTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.Op.COMMAND;

/**
 * Buffers all executions between each cycle in order to remove duplicates
 */
public class TaskExecutor extends Executioner {

    protected boolean trace;

    /**
     * if < 0, executes them all. 0 pauses, and finite value > 0 will cause them to be sorted first if the value exceeds the limit
     * interpreted as its integer value, although currently it is FloatParam
     */
    public final FloatParam exePerCycleMax = new FloatParam(-1);

    /**
     * temporary collection of tasks to remove after sampling
     */
    protected final FasterList<ITask> toRemove = new FasterList();

    /**
     * amount of priority to subtract from each processed task (re-calculated each cycle according to bag pressure)
     */
    protected float forgetEachPri;

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

    public TaskExecutor(int capacity) {
        super();
        active.capacity(capacity);
    }

    public TaskExecutor(int capacity, float executedPerCycle) {
        this(capacity);
        exePerCycleMax.setValue(Math.ceil(capacity * executedPerCycle));
    }

    @Override
    public void cycle(@NotNull NAR nar) {
        //flush();

        nar.eventCycleStart.emit(nar);

        flush();
    }

    @Override
    public int concurrency() {
        return 1;
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


    @Override
    public void forEach(Consumer<ITask> each) {
        active.forEachKey(each);
    }



    @Override
    public void runLater(Runnable r) {
        r.run(); //synchronous
    }

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

            float eFrac = ((float) toExe) / ps;
            float pAvg = (1f /*PForget.DEFAULT_TEMP*/) * active.depressurize(eFrac) / toExe;
            this.forgetEachPri = pAvg > Pri.EPSILON ? pAvg : 0;

            active.sample(toExe, this::actuallyRun);

            if (!toRemove.isEmpty()) {
                toRemove.clear(active::remove);
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

    protected void actuallyRun(ITask x) {
        ITask[] next;
        try {

            next = x.run(nar);

        } catch (Throwable e) {
            NAR.logger.error("{} {}", x, e.getMessage());
            toRemove.add(x); //TODO add to a 'bad' bag?
            return;
        }

        if (next == ITask.DeleteMe) {
            x.delete();
            toRemove.add(x);
        } else if (next == ITask.HideMe || x.isDeleted())
            toRemove.add(x);
        else if (forgetEachPri > 0)
            x.priSub(forgetEachPri);

        if (next != null)
            actuallyFeedback(x, next);
    }

    protected void actuallyFeedback(ITask x, ITask[] next) {
        nar.input(next);
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
