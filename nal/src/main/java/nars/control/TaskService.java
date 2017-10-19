package nars.control;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Service which reacts to NAR TaskProcess events
 */
abstract public class TaskService extends NARService implements BiConsumer<NAR, Task> {

    @Override
    protected void start(NAR nar) {
        super.start(nar);
        ons.add(nar.onTask((t) -> accept(nar, t)));
    }

    protected TaskService(@NotNull NAR nar) {
        super(nar);
    }

}
