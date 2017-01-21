package nars.web;

import nars.NAR;
import nars.Task;
import nars.bag.ArrayBag;
import nars.budget.BudgetMerge;
import nars.link.BLink;
import nars.link.DefaultBLink;
import nars.op.Leak;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 1/21/17.
 */
abstract public class LeakOut extends Leak<Task> {

    public LeakOut(NAR nar, int capacity, float rate) {
        super(new ArrayBag<Task>(capacity, BudgetMerge.plusBlend, new ConcurrentHashMap<>()), rate, nar);
    }

    //boolean echoCommandInput = false;

    @Override
    protected float onOut(@NotNull BLink<Task> t) {
        return send(t.get());
    }

    abstract protected float send(Task task);

    @Override
    protected void in(@NotNull Task t, Consumer<BLink<Task>> each) {
        if (t.isCommand()) {
            send(t); //immediate
        } else {
            float p = t.pri();
            if (p == p) { // || (t.term().containsTermRecursively(nar.self()) && (p > 0.5f))) {
                each.accept(new DefaultBLink<>(t, t));
            }
        }
    }
}
