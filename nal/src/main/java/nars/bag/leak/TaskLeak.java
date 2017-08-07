package nars.bag.leak;

import jcog.bag.Bag;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import nars.NAR;
import nars.Task;
import nars.control.TaskService;
import org.jetbrains.annotations.NotNull;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends TaskService  {

    protected final DtLeak<Task, PLink<Task>> leak;

    protected TaskLeak(@NotNull Bag<Task, PLink<Task>> bag, float ratePerDuration, @NotNull NAR n) {
        this(bag, new FloatParam(ratePerDuration), n);
    }

    TaskLeak(@NotNull Bag<Task, PLink<Task>> bag, @NotNull FloatParam rate, @NotNull NAR n) {
        super(n);
        this.leak = new DtLeak<>(bag, rate) {
            @Override
            protected float onOut(@NotNull PLink<Task> b) {
                return TaskLeak.this.leak(b.get());
            }
        };
    }

    @Override
    public void clear() {
        leak.clear();
    }

    @Override
    protected final void start(NAR nar) {
        super.start(nar);
        ons.add(nar.onCycle((nn) -> leak.commit(nn.time(), nn.dur())));
    }

    @Override
    public final void accept(NAR nar, Task t) {
        if (preFilter(t))
            leak.put(new PLink<>(t, t.priElseZero()));
    }

    protected boolean preFilter(Task x) {
        return true;
    }

    /** returns how much of the input was consumed; 0 means nothing, 1 means 100% */
    abstract protected float leak(Task out);
}
