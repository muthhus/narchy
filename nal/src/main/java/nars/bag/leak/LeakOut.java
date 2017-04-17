package nars.bag.leak;

import jcog.bag.impl.ArrayBag;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import jcog.pri.RawPLink;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 1/21/17.
 */
abstract public class LeakOut extends TaskLeak<Task,PLink<Task>> {

    public LeakOut(NAR nar, int capacity, float rate) {
        super(new ArrayBag<Task>(capacity, PriMerge.maxBlend, new ConcurrentHashMap<>()), rate, nar);
    }

    @Override protected float onOut(@NotNull PLink<Task> t) {
        return send(t.get());
    }

    abstract protected float send(Task task);

    @Override
    protected void in(@NotNull Task t, Consumer<PLink<Task>> each) {
        if (t.isCommand()) {
            send(t); //immediate
        } else {
            float p = t.pri();
            if (p == p) {
                each.accept(new RawPLink<>(t, t.priSafe(0)));
            }
        }
    }
}
