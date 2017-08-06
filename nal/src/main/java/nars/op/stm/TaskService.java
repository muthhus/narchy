package nars.op.stm;

import com.google.common.util.concurrent.AbstractIdleService;
import jcog.event.Ons;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Service which reacts to NAR TaskProcess events
 */
abstract public class TaskService extends AbstractIdleService implements Consumer<Task>, Termed {

    @NotNull
    public final NAR nar;
    boolean allowNonInput;
    protected Ons ons;


    @Override
    protected void startUp() throws Exception {
        ons = new Ons(
            nar.onTask(this),
            nar.eventClear.on(n -> clear())
        );
    }



    @Override
    protected void shutDown() throws Exception {
        ons.off();
        ons = null;
    }

    protected TaskService(@NotNull NAR nar) {
        super();
        this.nar = nar;
        nar.add(term(), this);
    }

    public void clear() {
        //default: nothing
    }

    @Override
    public abstract void accept(@NotNull Task t);

    @Override
    public @NotNull Term term() {
        return $.quote(getClass() + "@" + System.identityHashCode(this));
    }

}
