package nars.util.exe;

import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class SynchronousExecutor extends Executioner {


    @Override
    public int concurrency() {
        return 1;
    }


    @Override
    public void cycle(@NotNull NAR nar) {
        nar.eventCycleStart.emit(nar);
    }

    @Override
    public void run(@NotNull Consumer<NAR> r) {
        r.accept(nar);
    }

    @Override
    public void runLater(@NotNull Runnable r) {
        r.run();
    }

    @Override
    public void forEach(Consumer<ITask> each) {
        //nothing
    }

    @Override
    public boolean run(@NotNull ITask input) {
        ITask[] next = input.run(nar);
        if (next != null) {
            for (ITask x : next) {
                if (x == null)
                    break;
                run(x);
            }
        }
        return true;
    }

}
