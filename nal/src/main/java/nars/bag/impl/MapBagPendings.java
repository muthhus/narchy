package nars.bag.impl;

import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by me on 7/3/16.
 */
public class MapBagPendings<X> extends ArrayBag.BagPendings<X> {

    private final BudgetMerge merge;
    @NotNull
    private final Map<X, Budget> pending;
    int capacity;

    public MapBagPendings(BudgetMerge merge) {
        this.merge = merge;
        this.pending =
                new ConcurrentHashMap(capacity);
                //new ConcurrentHashMapUnsafe<>(capacity);
    }

    @Override
    public void clear() {
        pending.clear();
    }

//    protected Map<X, Budget> newPendingMap() {
//        //int s = 1+capacity/2;
//
//        //return new HashMap<>(capacity);
//        //return new HashMap<>();
//        //return new UnifriedMap<>(8); //<-- not safe, grows huge
//        //return new WeakHashMap<>(capacity);
//        //return new LinkedHashMap<>(capacity);
//        return new ConcurrentHashMapUnsafe<>(capacity);
//    }

    float sum;

    @Override
    public final float mass(ArrayBag<X> bag) {
        sum = 0;
        pending.forEach((k, v) -> sum += v.pri() * v.dur());
        return sum;
    }

    @Override
    public void capacity(int c) {
        this.capacity = c;
    }

    @Override
    public void add(X x, float p, float d, float q) {
        this.pending.merge(x, new RawBudget(p, d, q), merge);
    }


    @Override
    public int size() {
        return pending.size();
    }

    @Override
    public void apply(@NotNull ArrayBag<X> target) {
        Iterator<Map.Entry<X, Budget>> ii = pending.entrySet().iterator();
        while (ii.hasNext()) {
            Map.Entry<X, Budget> kb = ii.next();
            X k = kb.getKey();
            Budget b = kb.getValue();
            target.commitPending(k, b.pri(), b.dur(), b.qua());
            ii.remove();
        }
//        clear();
//        n.forEach((k, b) -> {
//            target.commitPending(k, b.pri(), b.dur(), b.qua());
//        });
    }
}
