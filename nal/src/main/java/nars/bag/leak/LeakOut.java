package nars.bag.leak;

import jcog.bag.impl.ArrayBag;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import jcog.pri.PLink;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 1/21/17.
 */
abstract public class LeakOut extends TaskLeak<Task,PriReference<Task>> {

    public LeakOut(NAR nar, int capacity, float rate) {
        super(new ArrayBag<>(capacity, PriMerge.max, new ConcurrentHashMap<>()), rate, nar);
    }

    @Override protected float onOut(@NotNull PriReference<Task> t) {
        return send(t.get());
    }

    abstract protected float send(Task task);

    @Override
    protected void in(@NotNull Task t, Consumer<PriReference<Task>> each) {
        if (t.isCommand()) {
            send(t); //immediate
        } else {
            float p = t.pri();
            if (p == p) {
                each.accept(new PLink<>(t, t.priSafe(0)));
            }
        }
    }
}
