package nars.bag.leak;

import jcog.bag.Bag;
import jcog.data.FloatParam;
import jcog.event.On;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.op.stm.TaskService;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends TaskService implements Consumer<Task> {

    protected final DtLeak<Task, PLink<Task>> leak;

    protected TaskLeak(@NotNull Bag<Task, PLink<Task>> bag, float ratePerDuration, @NotNull NAR n) {
        this(bag, new FloatParam(ratePerDuration), n);
    }

    TaskLeak(@NotNull Bag<Task, PLink<Task>> bag, @NotNull FloatParam rate, @NotNull NAR n) {
        super(n);
        this.leak = new DtLeak<>(bag, rate) {
            @Override
            protected float onOut(@NotNull PLink<Task> b) {
                return TaskLeak.this.onOut(b.get());
            }
        };
    }

    @Override
    public void clear() {
        leak.clear();
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        ons.add(nar.onCycle((nn) -> leak.commit(nn.time(), nn.dur())));
    }

    @Override
    public void accept(Task t) {
        leak.put(new PLink<>(t, t.pri()));
    }

    /** returns how much of the input was consumed; 0 means nothing, 1 means 100% */
    abstract protected float onOut(Task out);
}
