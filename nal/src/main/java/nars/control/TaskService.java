package nars.control;

import jcog.event.Ons;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Service which reacts to NAR TaskProcess events
 */
abstract public class TaskService extends NARService implements Consumer<Task> {

    protected Ons ons;


    @Override
    protected void startUp() throws Exception {
        super.startUp();
        ons.add(nar.onTask(this));
    }


    protected TaskService(@NotNull NAR nar) {
        super(nar);
    }


    @Override
    public abstract void accept(@NotNull Task t);


}
