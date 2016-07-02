package nars.bag.impl;

import nars.bag.WeakBudget;
import nars.budget.merge.BudgetMerge;
import nars.util.data.map.UnifriedMap;

import java.util.Map;

/**
 * Created by me on 7/2/16.
 */
public class WeakBudgetPendings<X> extends ArrayBag.BagPendings<X> {

    private /*Reference*/ WeakBudgetMap<X> pending;


//        protected Map<V, RawBudget> newPendingMap() {
//            //int s = 1+capacity/2;
//            int s = 4;
//            //return new HashMap<>();
//            return new WeakHashMap<>(s);
//            //return new LinkedHashMap<>(s);
//        }


    @Override
    public final float mass() {
        /*Reference*/
        WeakBudgetMap<X> pending = this.pending;
        if (pending != null) return pending.mass();
        return 0;
    }

    @Override
    public void clear() {
        pending = null;
    }

    @Override
    public void capacity(int c) {

    }

    @Override
    public void add(X x, float p, float d, float q, BudgetMerge merge) {
        //Reference<WeakBudgetMap<X>> m = this.pending;
        WeakBudgetMap<X> n = this.pending;
        if (n == null) {
            this.pending = /*reference*/(n = new WeakBudgetMap<>(newInternalMap()));
        } else {
            //n = m.get();
        }
        if (n != null)
            n.put(x, p, d, q, merge);
    }
//        protected Reference<WeakBudgetMap<X>> reference(WeakBudgetMap<X> w) {
//            //return new SoftReference<>(w);
//
//        }

    public Map<X, WeakBudget<X>> newInternalMap() {
        //return new HashMap();
        return new UnifriedMap<>(8);
    }

    @Override
    public int size() {
        if (pending != null)
            return pending.size();
        return 0;
    }

    @Override
    public void apply(ArrayBag<X> target) {
        WeakBudgetMap<X> n = this.pending;
        if (n != null) {
            this.pending = null;
            n.forEachBudget((b) -> {
                target.commitPending(b.get(), b.pri(), b.dur(), b.qua());
            });
            n.delete();
        }
    }
}
