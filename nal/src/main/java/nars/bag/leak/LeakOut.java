package nars.bag.leak;

import jcog.bag.impl.ConcurrentCurveBag;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 1/21/17.
 */
abstract public class LeakOut extends TaskLeak {

    protected LeakOut(NAR nar, int capacity, float rate) {
        super(
                //new PLinkArrayBag<>(capacity, PriMerge.max, new ConcurrentHashMap<>())
                new ConcurrentCurveBag<>(PriMerge.max, new ConcurrentHashMap(capacity), nar.random(), capacity)
                , rate, nar);
    }



    @Override protected float onOut(@NotNull Task t) {
        return send(t);
    }

    abstract protected float send(Task task);

    @Override
    public void accept(Task t) {
        if (t.isCommand()) {
            send(t); //immediate
        } else {
            float p = t.pri();
            if (p == p) {
                super.accept(t);
            }
        }
    }
}
