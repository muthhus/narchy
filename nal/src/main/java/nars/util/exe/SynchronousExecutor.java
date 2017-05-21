package nars.util.exe;

import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class SynchronousExecutor extends Executioner {

    //final ArrayDeque<Runnable> pending = new ArrayDeque<>(128 );

    boolean stopping;

    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public void stop() {
        stopping = true;
    }

    @Override
    public void cycle(@NotNull NAR nar) {

        //only execute the current set of pending Runnable's here. more may be added but they will be handled in the next frame
//        int p = pending.size();
//        for (int i = 0; i < p; i++) {
//            pending.removeFirst().run();
//        }

        nar.eventCycleStart.emit(nar);

        if (stopping) {
            nar = null;
            stopping = false;
        }
    }

    @Override
    public void run(@NotNull Consumer<NAR> r) {
        r.accept(nar);
    }

    @Override
    public void runLater(@NotNull Runnable r) {
        //pending.add/*Last*/(r);

        //try {
            r.run();
//        } catch (Throwable t) {
//            NAR.logger.error("{} {}", r, t.getMessage());
//        }
    }


    @Override public boolean run(@NotNull ITask input) {
        ITask[] next = input.run(nar);
        if (next!=null) {
            for (ITask x : next)
                run(x);
        }
        return true;
    }

}
