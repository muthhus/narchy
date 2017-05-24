package nars.util.exe;

import jcog.data.FloatParam;
import jcog.data.sorted.SortedArray;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Buffers all executions between each cycle in order to remove duplicates
 */
public class BufferedSynchronousExecutor extends SynchronousExecutor {

    final Queue<ITask> q;
    final Map<ITask, ITask> pending = new LinkedHashMap();

    /**
     * if < 0, executes them all. 0 pauses, and finite value > 0 will cause them to be sorted first if the value exceeds the limit
     */
    public final FloatParam maxExecutionsPerCycle = new FloatParam(-1);
    private SortedArray<ITask> sorted;

    public BufferedSynchronousExecutor() {
        this(new ArrayDeque<>());
    }

    public BufferedSynchronousExecutor(Queue<ITask> q) {
        this.q = q;
    }

    @Override
    public void cycle(@NotNull NAR nar) {
        flush();

        super.cycle(nar);

        flush(); //<- ? why does this have to be done twice
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
            if (q.isEmpty())
                return;

            ITask x;
            while (null != (x = q.poll())) {
                pending.merge(x, x, (p, X) -> {
                    if (p == X)
                        return p; //does this occurr?
                    return p.merge(X);
                });
            }

            int toExe = maxExecutionsPerCycle.intValue();
            if (toExe < 0 || toExe > pending.size()) {
                pending.values().forEach(this::actuallyRun);
                pending.clear();

            } else {
                //sort
                if (sorted == null || sorted.capacity() != (toExe + 1)) {
                    sorted = new SortedArray<ITask>(new ITask[toExe + 1]);
                }

                pending.values().forEach(s -> {
                    sorted.add(s, Prioritized::oneMinusPri);
                    if (sorted.size() > toExe)
                        sorted.removeLast();
                });
                pending.clear();
                assert (sorted.size() == toExe);
                sorted.forEach(this::actuallyRun);
            }


        } finally {
            busy.set(false);
        }
    }

    protected void actuallyRun(@NotNull ITask input) {
        super.run(input);
    }

    @Override
    public boolean run(@NotNull ITask input) {
        boolean b = q.offer(input);
        if (!b) {
            //try once more, removing the next item (FIFO)
            ITask removed = q.poll();
            return q.offer(input);
        }
        return true;
    }

}
