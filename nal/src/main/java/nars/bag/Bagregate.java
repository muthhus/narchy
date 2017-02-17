package nars.bag;

import jcog.data.FloatParam;
import nars.bag.impl.ArrayBag;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 *  a bag which wraps another bag, accepts its value as input but at a throttled rate
 *  resulting in containing effectively the integrated / moving average values of the input bag
 */
public class Bagregate<X> extends ArrayBag<X> {

    private final Iterable<? extends BLink<X>> src;
    private final MutableFloat scale;
    final AtomicBoolean busy = new AtomicBoolean();

    public Bagregate(@NotNull Iterable<? extends BLink<X>> src, int capacity, float scale) {
        super(capacity, BudgetMerge.avgBlend, new ConcurrentHashMap<>(capacity));

        this.src = src;
        this.scale = new FloatParam(scale);

        update();
    }

    protected void update() {
        if (!busy.compareAndSet(false, true))
            return;

        float scale = this.scale.floatValue();

        Iterator<? extends BLink<X>> ss = src.iterator();
        int count = 0;
        int limit = capacity;
        while (ss.hasNext() && count < limit) {
            BLink<X> x = ss.next();
            if (x!=null && include(x)) {
                if (put(x, scale, null)!=null)
                    count++;
            }
        }

        commit();

        busy.set(false);
    }

    /** can be overridden to filter entry */
    protected boolean include(BLink<X> x) {
        return true;
    }

    @Override
    public void forEach(Consumer<? super BLink<X>> action) {
        forEach(size(), action);
    }

    @Override
    public void forEach(int max, Consumer<? super BLink<X>> action) {
        update();
        super.forEach(max, action);
    }

}
