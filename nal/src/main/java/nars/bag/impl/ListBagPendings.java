package nars.bag.impl;

import nars.Param;
import nars.budget.RawBLink;
import nars.budget.merge.BudgetMerge;
import nars.util.data.list.CircularArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;

/**
 * Created by me on 7/3/16.
 */
public class ListBagPendings<X extends Comparable<X>> extends ArrayBag.BagPendings<X> implements Comparator<RawBLink<X>> {

    private final BudgetMerge merge;
    //public List<RawBLink<X>> pending = null;
    @Nullable CircularArrayList<RawBLink<X>> pending;
    final Object lock = new Object();
    private int capacity;

    public ListBagPendings(BudgetMerge m) {
        this.merge = m;
    }

    @Override
    public void capacity(int c) {
        this.capacity = c;
    }

    @Override
    public void add(@NotNull X x, float p, float d, float q) {

        synchronized (lock) { //HACK
            CircularArrayList<RawBLink<X>> pend = this.pending;
            if (pend == null) {
                //pending = Global.newArrayList(capacity);
                this.pending = pend = new CircularArrayList<>(capacity);
            } else if (pend.size() == capacity) {
                pend.removeFirst();
            }

            pend.add(new RawBLink<>(x, p, d, q));
        }

    }

    @Override
    public int size() {
        synchronized (lock) { //HACK
            CircularArrayList<RawBLink<X>> p = this.pending;
            if (p == null)
                return 0;
            return p.size();
        }
    }

    @Override
    public void apply(@NotNull ArrayBag<X> bag) {
        synchronized (lock) { //HACK
            CircularArrayList<RawBLink<X>> p = this.pending;
            if (p != null) {
                clear();
                for (int i = 0, pendingSize = p.size(); i < pendingSize; i++) {
                    RawBLink<X> w = p.getAndSet(i, null);
                    if (w != null) {
                        float wp = w.pri();
                        if (wp == wp) { //not deleted
                            bag.commitPending(w.x, wp, w.dur(), w.qua());
                        }
                    }
                }
            }
        }
    }

    @Override
    public int compare(@NotNull RawBLink<X> a, @NotNull RawBLink<X> b) {
        boolean adel = a.isDeleted();
        boolean bdel = b.isDeleted();
//                if (adel && bdel)
//                    return 0;
//                if (adel)
//                    return +1;
//                if (bdel)
//                    return +1;
//

        int cmp = a.x.compareTo(b.x);
        if (cmp == 0 && !adel && !bdel) {
            merge.merge(a, b, 1f);
            b.deleteFast();
        }
        return cmp;
    }


    @Override
    public float mass(ArrayBag<X> bag) {

        synchronized (lock) { //HACK
            CircularArrayList<RawBLink<X>> p = this.pending;
            if (p == null)
                return 0;


            float sum = 0;

            //HACK remove null entries
            for (int i = 0, pendingSize = p.size(); i < pendingSize; ) {
                RawBLink<X> w = p.get(i);
                if (w == null) {
                    p.remove(i);
                    pendingSize--;
                } else {
                    i++;
                }
            }

            Collections.sort(p, this);

            for (int i = 0, pendingSize = p.size(); i < pendingSize; i++) {
                RawBLink<X> w = p.get(i);
                if (w != null) {
                    float pp = w.priIfFiniteElseZero();
                    if (pp > 0) {
                        sum += pp * w.dur();
                    } else {
                        p.set(i, null);
                    }
                }
            }
            if (sum < Param.BUDGET_EPSILON) {
                clear();
            }
            return sum;
        }
    }

    @Override
    public void clear() {
        pending = null;
    }
}
