package nars.bag.leak;

import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentCurveBag;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.control.ThrottledService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends ThrottledService {

    protected final DtLeak<Task, PLink<Task>> in;

    protected TaskLeak(@NotNull Bag<Task, PLink<Task>> bag, float ratePerDuration, @NotNull NAR n) {
        this(bag, new FloatParam(ratePerDuration), n);
    }

    protected TaskLeak(int capacity, float ratePerDuration, @NotNull NAR n) {
        this(new ConcurrentCurveBag<>(PriMerge.max, new ConcurrentHashMap(capacity), n.random(), capacity), ratePerDuration, n);
    }

    TaskLeak(@NotNull Bag<Task, PLink<Task>> bag, @NotNull FloatParam rate, @NotNull NAR n) {
        super(n);
        this.in = new DtLeak<>(bag, rate) {
            @Override
            protected float receive(@NotNull PLink<Task> b) {
                Task t = b.get();
                if (t.isCommand() || t.isDeleted())
                    return 0f;
                else
                    return TaskLeak.this.leak(t);
            }
        };
    }

    @Override
    protected void start(NAR nar) {
        super.start(nar);
        ons.add(nar.onTask((t) -> {
            accept(nar, t);
        }));
    }


    @Override
    public void clear() {
        in.clear();
    }

    @Override
    protected float run(NAR nar, long dt, float work) {
        return in.commit(nar.time(), dt, nar.dur(),
                work/((float)in.bag.capacity()));
    }

    public final void accept(NAR nar, Task t) {
        if (preFilter(t))
            in.put(new PLink<>(t, t.priElseZero()));
    }



    protected boolean preFilter(Task next) {
        return true;
    }

    /** returns how much of the input was consumed; 0 means nothing, 1 means 100% */
    abstract protected float leak(Task next);
}
