package nars.util.exe;

import com.google.common.collect.Sets;
import nars.$;
import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Buffers all executions between each cycle in order to remove duplicates
 */
public class BufferedSynchronousExecutor extends SynchronousExecutor {

    final List<ITask> prepending = $.newArrayList();
    final LinkedHashMap<ITask,ITask> pending = new LinkedHashMap();

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

    @Override
    public void stop() {
        flush();
        super.stop();
    }

    private void flush() {
        if (!prepending.isEmpty()) {
            prepending.forEach(x -> {
               pending.merge(x, x, (p, X) -> {
                  p.priAdd(X.priSafe(0));
                  return p;
               });
            });
            prepending.clear();
            pending.values().forEach(super::run);
            pending.clear();
        }
    }

    @Override
    public void run(@NotNull ITask input) {
        prepending.add(input);
    }

}
