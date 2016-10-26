package nars.bag.impl;

import nars.$;
import nars.Param;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.Forget;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.util.data.sorted.SortedArray;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 */
public class ArrayBag<V> extends SortedListTable<V, BLink<V>> implements Bag<V>, BiFunction<RawBudget, RawBudget, RawBudget> {


    public final BudgetMerge mergeFunction;


    /**
     * inbound pressure sum since last commit
     */
    private volatile float pressure = 0;

    /**
     * mass as calculated in previous commit
     */
    private volatile float mass = 0;
    private static final Logger logger = LoggerFactory.getLogger(ArrayBag.class);

    public ArrayBag(@Deprecated int cap, BudgetMerge mergeFunction, @NotNull Map<V, BLink<V>> map) {
        super(BLink[]::new, map);

        this.mergeFunction = mergeFunction;
        this.capacity = cap;
    }


    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }


    /** returns true unless failed to add during 'add' operation */
    @Override protected boolean update(@Nullable BLink<V> toAdd) {



        SortedArray<BLink<V>> items = this.items;

        synchronized (items) {
            int additional = (toAdd != null) ? 1 : 0;

            int s = size();
            int c = capacity();

            if (s + additional > c) {
                s = clean(toAdd, s, additional);
                if (s + additional > c)
                    return false; //throw new RuntimeException("overflow");
            }

            if (toAdd != null) {

                //append somewhere in the items; will get sorted to appropriate location during next commit
                //TODO update range

//                Object[] a = items.array();

//                //scan for an empty slot at or after index 's'
//                for (int k = s; k < a.length; k++) {
//                    if ((a[k] == null) /*|| (((BLink)a[k]).isDeleted())*/) {
//                        a[k] = toAdd;
//                        items._setSize(s+1);
//                        return;
//                    }
//                }

                int ss = size();
                if (ss < capacity) {
                    items.addInternal(toAdd); //grows the list if necessary
                } else {
                    //throw new RuntimeException("list became full during insert");
                    return false;
                }

                float p = toAdd.pri();
                if (minPri < p) {
                    this.minPri = p;
                }


            }

        }

        return true;

//        if (toAdd != null) {
//            synchronized (items) {
//                //the item key,value should already be in the map before reaching here
//                items.add(toAdd, this);
//            }
//            modified = true;
//        }
//
//        if (modified)
//            updateRange(); //regardless, this also handles case when policy changed and allowed more capacity which should cause minPri to go to -1

    }

    private int clean(@Nullable BLink<V> toAdd, int s, int sizeThresh) {

        List<BLink<V>> toRemove = $.newArrayList(Math.max(0,s - sizeThresh));

        //first step: remove any nulls and deleted values
        s -= removeDeleted(toRemove);

        //second step: if still not enough, do a hardcore removal of the lowest ranked items until quota is met
        s = removeWeakestUntilUnderCapacity(s, toRemove, toAdd != null);


        //do full removal
        for (int i = 0, toRemoveSize = toRemove.size(); i < toRemoveSize; i++) {
            BLink<V> w = toRemove.get(i);

            V k = w.get();
            if (k == null)
                continue;

            BLink<V> k2 = map.remove(k);

            if (k2 != w && k2 != null) {
                //throw new RuntimeException(
                logger.error("bag inconsistency: " + w + " removed but " + k2 + " may still be in the items list");
                //reinsert it because it must have been added in the mean-time:
                map.putIfAbsent(k, k2);
            }

            onRemoved(w);

            w.delete();

        }
        return s;
    }

    private int removeWeakestUntilUnderCapacity(int s, @NotNull List<BLink<V>> toRemove, boolean pendingAddition) {
        SortedArray<BLink<V>> items = this.items;
        while (!isEmpty() && ((s - capacity()) + (pendingAddition ? 1 : 0)) > 0) {
            BLink<V> w = items.remove(s - 1);
            if (w != null)
                toRemove.add(w);
            s--;
        }
        return s;
    }

    @Nullable
    @Override
    public V add(Object key, float x) {
        BLink<V> c = map.get(key);
        if (c != null && !c.isDeleted()) {
            //float dur = c.dur();
            float pBefore = c.pri();
            c.priAdd(x);
            float delta = c.pri() - pBefore;
            pressure += delta;// * dur;
            return c.get();
        }
        return null;
    }

    @Override
    public V mul(Object key, float boost) {
        BLink<V> c = map.get(key);
        if (c != null && !c.isDeleted()) {
            //float dur = c.dur();
            float pBefore = c.pri();
            c.priMult(boost);
            float delta = c.pri() - pBefore;
            pressure += delta;// * dur;
            return c.get();
        }
        return null;

    }

    @Override
    public final float rank(BLink x) {
        return -cmp(x);
    }

    //    @Override
//    public final int compare(@Nullable BLink o1, @Nullable BLink o2) {
//        float f1 = cmp(o1);
//        float f2 = cmp(o2);
//
//        if (f1 < f2)
//            return 1;           // Neither val is NaN, thisVal is smaller
//        if (f1 > f2)
//            return -1;            // Neither val is NaN, thisVal is larger
//        return 0;
//    }


    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(@Nullable BLink o1, @Nullable BLink o2) {
        return cmpGT(o1, cmp(o2));
    }

    static final boolean cmpGT(@Nullable BLink o1, float o2) {
        return (cmp(o1) < o2);
    }

    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(float o1, @Nullable BLink o2) {
        return (o1 < cmp(o2));
    }


    /**
     * true iff o1 < o2
     */
    static final boolean cmpLT(@Nullable BLink o1, @Nullable BLink o2) {
        return cmpLT(o1, cmp(o2));
    }

    static final boolean cmpLT(@Nullable BLink o1, float o2) {
        return (cmp(o1) > o2);
    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    static float cmp(@Nullable Budgeted b) {
        return (b == null) ? -1f : b.priIfFiniteElseNeg1();

//        float p = b.pri();
//        return p == p ? p : -1f;
        //return (b!=null) ? b.priIfFiniteElseNeg1() : -1f;
        //return b.priIfFiniteElseNeg1();
    }


    @Override
    public final V key(@NotNull BLink<V> l) {
        return l.get();
    }


    @NotNull
    @Override
    public Bag<V> sample(int n, @NotNull Predicate<? super BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }


//    @Override
//    public final void put(@NotNull V key, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflow) {
//
//        float bp = b.pri();
//        if (bp != bp) { //deleted
//            putFail(key);
//            return;
//        }
//
//        float p = bp * scale;
//
//        BLink<V> existing = get(key);
//        if (existing != null) {
//            putExists(b, scale, existing, overflow);
//        } else {
//            //synchronized (map) {
//
//                if (minPriIfFull > p) {
//                    putFail(key);
//                    pending += p; //include failed input in pending
//                } else {
//                    putNewAndDeleteDisplaced(key, newLink(key, p, b.qua(), b.dur()));
//                }
//            //}
//        }
//
//
//    }

    @Override
    public final void put(@NotNull V key, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflow) {

        if (scale < Param.BUDGET_EPSILON)
            return; //too weak

        float bp = b.pri();
        if (bp != bp) { //already deleted
            return;
        }

        pressure += bp;

        Insertion ii = new Insertion(bp);

        BLink<V> v = map.compute(key, ii);

        int r = ii.result;
        switch (r) {
            case 0:
                float pBefore = v.pri();

                float o = mergeFunction.merge(v, b, scale);
                if (overflow != null)
                    overflow.add(o);

                float pAfter = v.pri();

                //technically this should be in a synchronized block but ...
                if (nars.util.Util.equals(minPri,pBefore,Param.BUDGET_EPSILON)) {
                    //in case the merged item determined the min priority
                    this.minPri = pAfter;
                }

                break;
            case +1:
                v.setBudget(bp * scale, b.dur(), b.qua());
                //v.set(key);
                if (update(v)) {
                    //success
                    onAdded(v);
                } else {
                    //failure, undo: remove the key from the map
                    map.remove(key);
                    onRemoved(null);
                }
                break;
            case -1:
                //reject due to insufficient budget
                onRemoved(null);
                break;
        }

    }

//    /**
//     * the applied budget will not become effective until commit()
//     */
//    @NotNull
//    protected final void putExists(@NotNull Budgeted b, float scale, @NotNull BLink<V> existing, @Nullable MutableFloat overflow) {
//
//
//
//    }

//    @NotNull
//    protected final BLink<V> newLink(@NotNull V i, @NotNull Budgeted b) {
//        return newLink(i, b, 1f);
//    }

//    @NotNull
//    protected final BLink<V> newLink(@NotNull V i, @NotNull Budgeted b, float scale) {
//        return newLink(i, scale * b.pri(), b.dur(), b.qua());
//    }


    @Nullable
    @Override
    protected BLink<V> addItem(BLink<V> i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public final Bag<V> commit() {

        if (!isEmpty()) {
            float existing = this.mass;
            Consumer<BLink> a;
            if (existing == 0) {
                a = null; //nothing to forget
                this.pressure = 0; //reset pending accumulator
            } else {
                float p = this.pressure;
                this.pressure = 0; //reset pending accumulator


                a = Forget.forget(p, existing, size());
            }

            commit(a);
        } else {
            minPri = -1;
        }

        return this;
    }


    /**
     * applies the 'each' consumer and commit simultaneously, noting the range of items that will need sorted
     */
    @NotNull
    @Override
    public Bag<V> commit(@Nullable Consumer<BLink> each) {

        int s = size();
        if (s > 0) {

            synchronized (items) {

                update(null);

                int lowestUnsorted = updateExisting(each, s);

                if (lowestUnsorted != -1) {
                    qsort(new int[24 /* estimate */], items.array(), 0 /*dirtyStart - 1*/, s-1);
                } // else: perfectly sorted

                updateRange();

            }

        }


        return this;
    }





    public float minPri = -1;

//    private final float minPriIfFull() {
//        BLink<V>[] ii = items.last();
//        BLink<V> b = ii[ii.length - 1];
//        if (b!=null) {
//            return b.priIfFiniteElseNeg1();
//        }
//        return -1f;
//
//        //int s = size();
//        //return (s == capacity()) ? itempriMin() : -1f;
//    }

    /**
     * returns the index of the lowest unsorted item
     */
    private int updateExisting(@Nullable Consumer<BLink> each, int s) {
//        int dirtyStart = -1;
        int lowestUnsorted = -1;


        BLink<V>[] l = items.array();
        int i = s - 1;
        float m = 0;
        //@NotNull BLink<V> beneath = l[i]; //compares with self below to avoid a null check in subsequent iterations
        float beneath = Float.POSITIVE_INFINITY;
        for (; i >= 0; ) {
            BLink<V> b = l[i];

            float bCmp;
            bCmp = b != null ? b.priIfFiniteElseNeg1() : -2; //sort nulls to the end of the end

            if (bCmp > 0) {
                if (each!=null)
                    each.accept(b);
                m += bCmp;
            }


            if (lowestUnsorted == -1 && bCmp < beneath) {
                lowestUnsorted = i + 1;
            }

            beneath = bCmp;
            i--;
        }

        this.mass = m;

        return lowestUnsorted;
    }

    /**
     * must be called in synchronized(items) block
     */
    private int removeDeleted(@NotNull List<BLink<V>> removed) {


        SortedArray<BLink<V>> items = this.items;
        Object[] l = items.array();
        int removedFromMap = 0;
        for (int s = items.size()-1; s >= 0; s--) {
            BLink<V> x = (BLink<V>) l[s];
            if (x == null || x.isDeleted()) {
                items.removeFast(s);
                if (x!=null)
                    removed.add(x);
                removedFromMap++;
            }
        }

        return removedFromMap;
    }

    @Override
    public void clear() {
        super.clear();
        updateRange();
    }

    private final void updateRange() {
        int s = size();
        this.minPri = s > 0 ? get(s - 1).priIfFiniteElseNeg1() : -1;
    }


    @Nullable
    @Override
    public RawBudget apply(@Nullable RawBudget bExisting, RawBudget bNext) {
        if (bExisting != null) {
            mergeFunction.merge(bExisting, bNext, 1f);
            return bExisting;
        } else {
            return bNext;
        }
    }

    @Override
    public void forEach(Consumer<? super BLink<V>> action) {
        Object[] x = items.array();
        if (x.length > 0) {
            for (BLink a : ((BLink[]) x)) {
                if (a != null) {
                    BLink<V> b = a;
                    if (!b.isDeleted())
                        action.accept(b);
                }
            }
        }
    }


    /**
     * http://kosbie.net/cmu/summer-08/15-100/handouts/IterativeQuickSort.java
     */

    public static void qsort(int[] stack, BLink[] c, int left, int right) {
        int stack_pointer = -1;
        while (true) {
            int i, j;
            if (right - left <= 7) {
                BLink swap;
                //bubble sort on a region of right less than 8?
                for (j = left + 1; j <= right; j++) {
                    swap = c[j];
                    i = j - 1;
                    float swapV = cmp(swap);
                    while (i >= left && cmpGT(c[i], swapV)) {
                        swap(c, i+1, i);
//                        BLink x = c[i];
//                        c[i] = c[i + 1];
//                        c[i + 1] = x;
                        i--;
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left = stack[stack_pointer--];
                } else {
                    break;
                }
            } else {
                BLink swap;

                int median = (left + right) >> 1;
                i = left + 1;
                j = right;

                swap(c, i, median);

                if (cmpGT(c[left], c[right])) {
                    swap(c, right, left);
                }
                if (cmpGT(c[i], c[right])) {
                    swap(c, right, i);
                }
                if (cmpGT(c[left], c[i])) {
                    swap(c, i, left);
                }

                {
                    BLink temp = c[i];
                    float tempV = cmp(temp);

                    while (true) {
                        while (cmpLT(c[++i], tempV)) ;
                        while (cmpGT(c[--j], tempV)) ;
                        if (j < i) {
                            break;
                        }
                        swap(c, j, i);
                    }

                    c[left + 1] = c[j];
                    c[j] = temp;
                }

                int a, b;
                if ((right - i + 1) >= (j - left)) {
                    a = i;
                    b = right;
                    right = j - 1;
                } else {
                    a = left;
                    b = j - 1;
                    left = i;
                }

                stack[++stack_pointer] = a;
                stack[++stack_pointer] = b;
            }
        }
    }

    public static void swap(BLink[] c, int x, int y) {
        BLink swap;
        swap = c[y];
        c[y] = c[x];
        c[x] = swap;
    }

    //    final Comparator<? super BLink<V>> comparator = (a, b) -> {
//        return Float.compare(items.score(b), items.score(a));
//    };


//        if (!v.hasDelta()) {
//            return;
//        }
//
////
////        int size = ii.size();
////        if (size == 1) {
////            //its the only item
////            v.commit();
////            return;
////        }
//
//        SortedIndex ii = this.items;
//
//        int currentIndex = ii.locate(v);
//
//        v.commit(); //after finding where it was, apply its updates to find where it will be next
//
//        if (currentIndex == -1) {
//            //an update for an item which has been removed already. must be re-inserted
//            put(v.get(), v);
//        } else if (ii.scoreBetween(currentIndex, ii.size(), v)) { //has position changed?
//            ii.reinsert(currentIndex, v);
//        }
//        /*} else {
//            //otherwise, it remains in the same position and a move is unnecessary
//        }*/
//    }


    @NotNull
    @Override
    public String toString() {
        //synchronized (map) {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
        //}
    }


    @Override
    public float priMax() {
        BLink<V> x = items.first();
        return x != null ? x.pri() : 0f;
    }

    @Override
    public float priMin() {
        BLink<V> x = items.last();
        return x != null ? x.pri() : 0f;
    }


//    public final void popAll(@NotNull Consumer<BLink<V>> receiver) {
//        forEach(receiver);
//        clear();
//    }

//    public void pop(@NotNull Consumer<BLink<V>> receiver, int n) {
//        if (n == size()) {
//            //special case where size <= inputPerCycle, the entire bag can be flushed in one operation
//            popAll(receiver);
//        } else {
//            for (int i = 0; i < n; i++) {
//                receiver.accept(pop());
//            }
//        }
//    }

//    public final float priAt(int cap) {
//        return size() <= cap ? 1f : item(cap).pri();
//    }
//

//    public final static class BudgetedArraySortedIndex<X extends Budgeted> extends ArraySortedIndex<X> {
//        public BudgetedArraySortedIndex(int capacity) {
//            super(1, capacity);
//        }
//
//
//        @Override
//        public float score(@NotNull X v) {
//            return v.pri();
//        }
//    }

    /**
     * Created by me on 8/15/16.
     */
    final class Insertion<V> implements BiFunction<V, BLink, BLink> {


        private final float pri;

        /**
         * TODO this field can be re-used for 'activated' return value
         * -1 = deactivated, +1 = activated, 0 = no change
         */
        int result = 0;

        public Insertion(float pri) {
            this.pri = pri;
        }


        @Nullable
        @Override
        public BLink apply(@NotNull Object key, @Nullable BLink existing) {


            if (existing != null) {
                //result=0
                return existing;
            } else {
                if (size()>=capacity && minPri > pri) {
                    this.result = -1;
                    return null;
                } else {
                    //accepted for insert
                    this.result = +1;
                    BLink b = newLink(key);
                    return b;
                }
            }
        }
    }
}


//        if (dirtyStart != -1) {
//            //Needs sorted
//
//            int dirtyRange = 1 + dirtyEnd - dirtyStart;
//
//            if (dirtyRange == 1) {
//                //Special case: only one unordered item; remove and reinsert
//                BLink<V> x = items.remove(dirtyStart); //remove directly from the decorated list
//                items.add(x); //add using the sorted list
//
//            } else if ( dirtyRange < Math.max(1, reinsertionThreshold * s) ) {
//                //Special case: a limited number of unordered items
//                BLink<V>[] tmp = new BLink[dirtyRange];
//
//                for (int k = 0; k < dirtyRange; k++) {
//                    tmp[k] = items.remove( dirtyStart /* removal position remains at the same index as items get removed */);
//                }
//
//                //TODO items.get(i) and
//                //   ((FasterList) items.list).removeRange(dirtyStart+1, dirtyEnd);
//
//                for (BLink i : tmp) {
//                    if (i.isDeleted()) {
//                        removeKeyForValue(i);
//                    } else {
//                        items.add(i);
//                    }
//                }
//
//            } else {
//            }
//        }
