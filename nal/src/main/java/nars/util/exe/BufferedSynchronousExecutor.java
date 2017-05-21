package nars.util.exe;

import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Buffers all executions between each cycle in order to remove duplicates
 */
public class BufferedSynchronousExecutor extends SynchronousExecutor {

    final Queue<ITask> q;
    final Map<ITask, ITask> pending = new LinkedHashMap();


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

        flush();
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

    private void flush() {
        if (!q.isEmpty()) {
            ITask x;
            while (null != (x = q.poll())) {
                pending.merge(x, x, (p, X) -> {
                    if (p == X)
                        return p; //does this occurr?


                    return p.merge(X);
                });
            }
            pending.values().forEach(this::actuallyRun);
            pending.clear();
        }
    }

    protected void actuallyRun(@NotNull ITask input) {
//        if (input instanceof PremiseBuilder.DerivePremise)
//            System.out.println(input);
        super.run(input);
    }

    @Override
    public boolean run(@NotNull ITask input) {
        return q.offer(input);
    }

}
