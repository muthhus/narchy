package nars.bag.leak;

import nars.NAR;
import nars.Task;
import nars.bag.impl.ArrayBag;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.budget.RawBLink;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 1/21/17.
 */
abstract public class LeakOut extends Leak<Task,BLink<Task>> {

    public LeakOut(NAR nar, int capacity, float rate) {
        super(new ArrayBag<Task>(capacity, BudgetMerge.maxBlend, new ConcurrentHashMap<>()), rate, nar);
    }

    @Override protected float onOut(@NotNull BLink<Task> t) {
        return send(t.get());
    }

    abstract protected float send(Task task);

    @Override
    protected void in(@NotNull Task t, Consumer<BLink<Task>> each) {
        if (t.isCommand()) {
            send(t); //immediate
        } else {
            float p = t.pri();
            if (p == p) {
                each.accept(new RawBLink<>(t, t));
            }
        }
    }
}
