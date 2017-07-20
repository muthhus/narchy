package nars.bag.leak;

import jcog.bag.Bag;
import jcog.data.FloatParam;
import jcog.event.On;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak</* TODO: A, */X, V extends PriReference<X>> extends DtLeak<X, V> implements Consumer<Task> {

    private final On onTask, onReset, onCycle;

    public  TaskLeak(@NotNull Bag<X, V> bag, float ratePerDuration, @NotNull NAR n) {
        this(bag, new FloatParam(ratePerDuration), n);
    }

    TaskLeak(@NotNull Bag<X, V> bag, @NotNull FloatParam rate, @NotNull NAR n) {
        super(bag, rate);
        onTask = n.onTask(this);
        onReset = n.eventClear.on((nn) -> clear());
        onCycle = n.onCycle((nn) -> commit(nn.time(), nn.dur()));
    }
    public void stop() {
        onTask.off();
        onReset.off();
        onCycle.off();
    }

    @Override
    public void accept(Task task) {
        in(task, bag::put);
    }

    /**
     * transduce an input to a series of created BLink's to be inserted
     */
    abstract protected void in(@NotNull Task task, Consumer<V> each);



}
