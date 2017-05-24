package nars.util.exe;

import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.data.FloatParam;
import jcog.data.sorted.SortedArray;
import jcog.pri.Prioritized;
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
public class BufferedSynchronousExecutorHijack extends SynchronousExecutor {

    final PriorityHijackBag<ITask, ITask> pending = new PriorityHijackBag<ITask, ITask>(3) {
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


    /**
     * if < 0, executes them all. 0 pauses, and finite value > 0 will cause them to be sorted first if the value exceeds the limit
     */
    public final FloatParam maxExecutionsPerCycle = new FloatParam(-1);
    //private SortedArray<ITask> sorted;

    public BufferedSynchronousExecutorHijack(int capacity) {
        super();
        pending.capacity(capacity);
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
            int ps = pending.size();
            if (ps == 0)
                return;

            boolean t = this.trace;
            if (t)
                pending.print();

            int toExe = maxExecutionsPerCycle.intValue();
            if (toExe < 0)
                toExe = pending.capacity();

            pending.sample(toExe, this::actuallyRun);
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

            pending.commit();

        } finally {
            busy.set(false);
        }
    }

    protected void actuallyRun(@NotNull ITask input) {
        super.run(input);
    }

    @Override
    public boolean run(@NotNull ITask input) {
        if (input.punc() == COMMAND) {
            return super.run(input); //commands executed immediately
        } else {
            return pending.put(input) != null;
        }
    }

}
