package nars.nar.exe;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;

/**
 * Created by me on 8/16/16.
 */
public final class SingleThreadExecutioner extends Executioner {

    final ArrayDeque<Runnable> pending = new ArrayDeque<>(8192);


    @Override
    public final int concurrency() {
        return 1;
    }


    @Override
    public void next(@NotNull NAR nar) {

        nar.eventFrameStart.emit(nar);

        //only execute the current set of pending Runnable's here. more may be added but they will be handled in the next frame
        int p = pending.size();
        for (int i = 0; i < p; i++) {
            pending.removeFirst().run();
        }

    }

    @Override
    public final void execute(@NotNull Runnable r) {
        pending.add/*Last*/(r);
    }

    @Override
    public void inputLater(Task[] t) {
        //just execute here in this thread
        nar.input(t);
    }

    @Override
    public boolean executeMaybe(Runnable r) {
        execute(r);
        return true;
    }
}
