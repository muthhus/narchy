package nars.nar;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

/**
 * Created by me on 8/16/16.
 */
abstract public class Executioner implements Executor {
    abstract public void start(@NotNull NAR nar);

    abstract public void synchronize();

    abstract public void inputLater(@NotNull Task[] t);

    abstract public void next(@NotNull NAR nar);

    abstract public boolean executeMaybe(Runnable r);

    abstract public void stop();

}
