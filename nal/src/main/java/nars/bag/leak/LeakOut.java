package nars.bag.leak;

import jcog.bag.impl.ConcurrentCurveBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by me on 1/21/17.
 */
abstract public class LeakOut extends TaskLeak {

    public LeakOut(NAR nar, int capacity, float rate) {
        super(
                //new PLinkArrayBag<>(capacity, PriMerge.max, new ConcurrentHashMap<>())
                new ConcurrentCurveBag<>(PriMerge.max, new ConcurrentHashMap(capacity), nar.random(), capacity)
                , rate, nar);
    }


    @Override
    public boolean preFilter(Task t) {
        if (t.isCommand()) {
            leak(t); //immediate
        } else {
            float p = t.pri();
            return p==p;
        }
        return false;
    }
}
