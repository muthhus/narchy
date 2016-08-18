package nars.nar;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/16/16.
 */
public class SingleThreadExecutioner extends Executioner {

    private NAR nar;

    @Override
    public void start(NAR nar) {
        this.nar = nar;
    }

    @Override
    public void stop() {

    }

    @Override
    public void synchronize() {

    }

    @Override
    public void next(@NotNull NAR nar) {
        nar.eventFrameStart.emit(nar);
    }

    @Override
    public void execute(@NotNull Runnable r) {
        r.run();
    }

    @Override
    public void inputLater(Task[] t) {
        nar.input(t);
    }

    @Override
    public boolean executeMaybe(Runnable r) {
        r.run();
        return true;
    }
}
