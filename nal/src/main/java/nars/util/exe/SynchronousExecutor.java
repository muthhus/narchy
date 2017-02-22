package nars.util.exe;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/16/16.
 */
public class SynchronousExecutor extends Executioner {

    //final ArrayDeque<Runnable> pending = new ArrayDeque<>(128 );

    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public final void cycle(@NotNull NAR nar) {

        //only execute the current set of pending Runnable's here. more may be added but they will be handled in the next frame
//        int p = pending.size();
//        for (int i = 0; i < p; i++) {
//            pending.removeFirst().run();
//        }

        nar.eventCycleStart.emit(nar);

    }

    @Override
    public final void run(@NotNull Runnable r) {
        //pending.add/*Last*/(r);

        try {
            r.run();
        } catch (Throwable t) {
            NAR.logger.error("{} {}", r, t);
        }
    }

    @Override
    public final void run(Task[] t) {
        //just execute here in this thread
        nar.input(t);
    }


}
