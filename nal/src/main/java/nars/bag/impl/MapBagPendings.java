package nars.bag.impl;

import com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;

import java.util.Map;

/**
 * Created by me on 7/3/16.
 */
public class MapBagPendings<X> extends ArrayBag.BagPendings<X> {

    static boolean nullify = false;
    private final BudgetMerge merge;
    private /*Reference*/ Map<X, Budget> pending;
    int capacity;


    public MapBagPendings(BudgetMerge merge) {
        this.merge = merge;
    }

    @Override
    public void clear() {
        if (nullify)
            pending = null;
        else if (pending != null)
            pending.clear();
    }

    protected Map<X, Budget> newPendingMap() {
        //int s = 1+capacity/2;

        //return new HashMap<>(capacity);
        //return new HashMap<>();
        //return new UnifriedMap<>(8); //<-- not safe, grows huge
        //return new WeakHashMap<>(capacity);
        //return new LinkedHashMap<>(s);
        return new ConcurrentHashMapUnsafe<>(capacity);
    }

    float sum;

    public final float mass(ArrayBag<X> bag) {
        sum = 0;
        if (pending != null)
            pending.forEach((k, v) -> sum += v.pri() * v.dur());
        return sum;
    }

    @Override
    public void capacity(int c) {
        this.capacity = c;
    }

    @Override
    public void add(X x, float p, float d, float q) {

        Map<X, Budget> n = this.pending;
        if (n == null) {
            this.pending = n = newPendingMap();
        }
        n.merge(x, new RawBudget(p, d, q), merge);
    }


    @Override
    public int size() {
        if (pending != null)
            return pending.size();
        return 0;
    }

    @Override
    public void apply(ArrayBag<X> target) {
        Map<X, Budget> n = this.pending;
        if (n != null) {
            clear();
            n.forEach((k, b) -> {
                target.commitPending(k, b.pri(), b.dur(), b.qua());
            });
        }
    }
}
