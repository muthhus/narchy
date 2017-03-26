package nars.bag.impl;

import jcog.bag.Bag;
import jcog.bag.Prioritized;
import jcog.data.sorted.SortedArray;
import jcog.table.SortedListTable;
import nars.$;
import nars.Param;
import nars.attention.Forget;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 */
public class ArrayBag<X> extends SortedListTable<X, BLink<X>> implements Bag<X, BLink<X>> {

//    @Deprecated @NotNull
//    public static <K> BLink<K> newLink(@NotNull K i, @Nullable Budgeted exists) {
//
//        if (exists == null || exists.isDeleted()) {
//            if (i instanceof Budgeted) {
//                return new DependentBLink((Budgeted) i);
//                //return new WeakBLink(i);
//            } else {
//                return new DefaultBLink(i);
//            }
//        } else {
//            return new DefaultBLink(i, exists);
//        }
//
//    }

    public final BudgetMerge mergeFunction;


    /**
     * inbound pressure sum since last commit
     */
    public float pressure, mass;


    public ArrayBag(BudgetMerge mergeFunction, @NotNull Map<X, BLink<X>> map) {
        this(0, mergeFunction, map);
    }

    public ArrayBag(@Deprecated int cap, BudgetMerge mergeFunction, @NotNull Map<X, BLink<X>> map) {
        super(BLink[]::new, map);

        this.mergeFunction = mergeFunction;
        this.capacity = cap;
    }

    /**
     * returns whether the capacity has changed
     */
    @Override
    public final boolean setCapacity(int newCapacity) {
        if (newCapacity != this.capacity) {
            this.capacity = newCapacity;
            synchronized (items) {
                if (this.size() > newCapacity)
                    commit(null, true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void pressurize(float f) {
        pressure += f;
    }

    /**
     * returns true unless failed to add during 'add' operation or is empty
     */
    @Override
    protected boolean updateItems(@Nullable BLink<X> toAdd) {


        SortedArray<BLink<X>> items;

        //List<BLink<V>> pendingRemoval;
        List<BLink<X>> pendingRemoval;
        boolean result;
        synchronized (items = this.items) {
            int additional = (toAdd != null) ? 1 : 0;
            int c = capacity();

            int s = size();

            int nextSize = s + additional;
            if (nextSize > c) {
                pendingRemoval = $.newArrayList(nextSize - c);
                s = clean(toAdd, s, nextSize - c, pendingRemoval);
                if (s + additional > c) {
                    clean2(pendingRemoval);
                    return false; //throw new RuntimeException("overflow");
                }
            } else {
                pendingRemoval = null;
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
                if (ss < c) {
                    items.add(toAdd, this);
                    result = true;
                    //items.addInternal(toAdd); //grows the list if necessary
                } else {
                    //throw new RuntimeException("list became full during insert");
                    result = false;
                }

//                float p = toAdd.pri();
//                if (minPri < p && capacity()<=size()) {
//                    this.minPri = p;
//                }


            } else {
                result = size() > 0;
            }

        }

        if (pendingRemoval != null)
            clean2(pendingRemoval);

        return result;

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

    private int clean(@Nullable BLink<X> toAdd, int s, int minRemoved, List<BLink<X>> trash) {

        final int s0 = s;

        if (cleanDeletedEntries()) {
            //first step: remove any nulls and deleted values
            s -= removeDeleted(trash, minRemoved);

            if (s0 - s >= minRemoved)
                return s;
        }

        //second step: if still not enough, do a hardcore removal of the lowest ranked items until quota is met
        s = removeWeakestUntilUnderCapacity(s, trash, toAdd != null);

        return s;
    }


    /**
     * return whether to clean deleted entries prior to removing any lowest ranked items
     */
    protected boolean cleanDeletedEntries() {
        return false;
    }

    private void clean2(List<BLink<X>> trash) {
        int toRemoveSize = trash.size();
        if (toRemoveSize > 0) {

            for (int i = 0; i < toRemoveSize; i++) {
                BLink<X> w = trash.get(i);

                X k = w.get();


                map.remove(k);

//                    if (k2 != w && k2 != null) {
//                        //throw new RuntimeException(
//                        logger.error("bag inconsistency: " + w + " removed but " + k2 + " may still be in the items list");
//                        //reinsert it because it must have been added in the mean-time:
//                        map.putIfAbsent(k, k2);
//                    }

                //pressure -= w.priIfFiniteElseZero(); //release pressure
                onRemoved(w);
                w.delete();

            }

        }


    }

    private int removeWeakestUntilUnderCapacity(int s, @NotNull List<BLink<X>> toRemove, boolean pendingAddition) {
        SortedArray<BLink<X>> items = this.items;
        final int c = capacity;
        while (!isEmpty() && ((s - c) + (pendingAddition ? 1 : 0)) > 0) {
            BLink<X> w = items.remove(s - 1);
            if (w != null) //skip over nulls
                toRemove.add(w);
            s--;
        }
        return s;
    }


    @Override
    public final float floatValueOf(BLink x) {
        return -pCmp(x);
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
    static boolean cmpGT(@Nullable BLink o1, @Nullable BLink o2) {
        return cmpGT(o1, pCmp(o2));
    }

    static boolean cmpGT(@Nullable BLink o1, float o2) {
        return (pCmp(o1) < o2);
    }

    /**
     * true iff o1 > o2
     */
    static boolean cmpGT(float o1, @Nullable BLink o2) {
        return (o1 < pCmp(o2));
    }


    /**
     * true iff o1 < o2
     */
    static boolean cmpLT(@Nullable BLink o1, @Nullable BLink o2) {
        return cmpLT(o1, pCmp(o2));
    }

    static boolean cmpLT(@Nullable BLink o1, float o2) {
        return (pCmp(o1) > o2);
    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    static float pCmp(@Nullable Prioritized b) {
        return (b == null) ? -2f : b.priSafe(-1); //sort nulls beneath

//        float p = b.pri();
//        return p == p ? p : -1f;
        //return (b!=null) ? b.priIfFiniteElseNeg1() : -1f;
        //return b.priIfFiniteElseNeg1();
    }


    @Override
    @NotNull public final X key(@NotNull BLink<X> l) {
        return l.get();
    }


    @NotNull
    @Override
    public ArrayBag<X> sample(int n, @NotNull Predicate<? super BLink<X>> target) {
        if (!isEmpty())
            forEachWhile(target, n);
        return this;
    }


    @Override
    public final BLink<X> put(@NotNull BLink<X> b, float scale, @Nullable MutableFloat overflow) {


        final boolean[] isNew = {false};

        pressure += b.pri() * scale;

        X key = key(b);
        BLink<X> v = map.compute(key, (kk, existing) -> {
            BLink<X> res;
            float o;
            if (existing != null && !existing.isDeleted()) {
                //merge
                res = existing;
                o = mergeFunction.merge(existing, b, scale);
            } else {
                //new
                BLink<X> n = b.cloneZero(b.qua());
                float oo = mergeFunction.merge(n, b, scale);
                float np = n.pri();

                if (size() >= capacity && np < priMin()) {
                    res = null; //failed insert
                    o = 0;
                } else {
                    isNew[0] = true;
                    res = n;
                    o = oo;
                }
            }

            if ((o > 0) && overflow != null) {
                overflow.add(o);
            }


            return res;

        });

        if (v == null) {
            return null; //rejected
        }

        if (isNew[0]) {

            //attempt new insert
            boolean added;
            synchronized (items) {

                added = updateItems(v);

                if (!added) {
                    v.delete();
                } else {
                    sortAfterUpdate();
                }
            }

            if (added) {
                onAdded(v);
                return v;
            }

            map.remove(key);
            return null; //reject

        } else {
            synchronized (items) {
                sortAfterUpdate(); //TODO see if this works better here, being an adaptive sort: https://en.wikipedia.org/wiki/Smoothsort
            }
            return v;
        }


    }


    @Nullable
    @Override
    protected BLink<X> addItem(@NotNull BLink<X> i) {
        throw new UnsupportedOperationException();
    }


    @Override
    @Deprecated
    public Bag<X, BLink<X>> commit() {
        float p = this.pressure;
        if (p > 0) {
            this.pressure = 0;
            return commit(Forget.forget(size(), p, mass, Forget::new));
        }
        return this;
    }

    @Override
    @NotNull
    public final ArrayBag<X> commit(Consumer<BLink<X>> update) {
        commit(update, false);
        return this;
    }

    private void commit(@Nullable Consumer<BLink<X>> update, boolean checkCapacity) {

        if (update != null || checkCapacity)
            update(update, checkCapacity);

        if (update != null) {
            float mass = 0;
            synchronized (items) {
                int iii = size();
                for (int i = 0; i < iii; i++) {
                    BLink x = get(i);
                    if (x != null)
                        mass += x.priSafe(0);
                }
            }
            this.mass = mass;
        }

    }


    /**
     * applies the 'each' consumer and commit simultaneously, noting the range of items that will need sorted
     */
    @NotNull
    protected ArrayBag<X> update(@Nullable Consumer<BLink<X>> each, boolean checkCapacity) {

        if (each != null)
            this.pressure = 0; //reset pressure accumulator

        synchronized (items) {

            if (size() > 0) {
                if (checkCapacity)
                    if (!updateItems(null))
                        return this;

                boolean needsSort;
                if (each != null) {
                    needsSort = !updateBudget(each);
                } else {
                    needsSort = true;
                }


                if (needsSort) {
                    sort();
                }

            }

        }

        return this;
    }


    protected void sortAfterUpdate() {
        sort();
    }

    public void sort() {
        int s = size();
        if (s > 0)
            qsort(new int[sortSize(s) /* estimate */], items.array(), 0 /*dirtyStart - 1*/, (s - 1));
    }

    protected static int sortSize(int s) {
        //estimate, probably log2(size)
        if (s < 128) {
            return 8;
        } else if (s < 2048) {
            return 16;
        } else {
            return 32;
        }
    }

    /**
     * returns whether the items list was detected to be sorted
     */
    private boolean updateBudget(@NotNull Consumer<BLink<X>> each) {
//        int dirtyStart = -1;
        boolean sorted = true;


        int s = size();
        BLink[] l = items.array();
        //@NotNull BLink<V> beneath = l[i]; //compares with self below to avoid a null check in subsequent iterations
        float pBelow = -2;
        for (int i = s - 1; i >= 0; ) {
            BLink b = l[i];

            float p = (b != null) ? b.priSafe(-1) : -2; //sort nulls to the end of the end

            if (p > 0) {
                each.accept(b);
            }

            if (pBelow - p >= Param.BUDGET_EPSILON) {
                sorted = false;
            }

            pBelow = p;
            i--;
        }


        return sorted;
    }


    private int removeDeleted(@NotNull Collection<BLink<X>> removed, int minRemoved) {

        SortedArray<BLink<X>> items = this.items;
        final Object[] l = items.array();
        int removedFromMap = 0;

        //iterate in reverse since null entries should be more likely to gather at the end
        for (int s = size() - 1; removedFromMap < minRemoved && s >= 0; s--) {
            BLink<X> x = (BLink<X>) l[s];
            if (x == null || x.isDeleted()) {
                items.removeFast(s);
                if (x != null)
                    removed.add(x);
                removedFromMap++;
            }
        }

        return removedFromMap;
    }

    @Override
    public void clear() {
        synchronized (items) {
            //map is possibly shared with another bag. only remove the items from it which are present in items
            items.forEach(x -> {
                map.remove(x.get());
                onRemoved(x);
            });
            items.clear();
        }
    }


//    @Nullable
//    @Override
//    public RawBudget apply(@Nullable RawBudget bExisting, RawBudget bNext) {
//        if (bExisting != null) {
//            mergeFunction.merge(bExisting, bNext, 1f);
//            return bExisting;
//        } else {
//            return bNext;
//        }
//    }


    @Override
    public float pri(@NotNull BLink<X> key) {
        return key.pri();
    }

    @Override
    public void forEachKey(@NotNull Consumer<? super X> each) {
        forEach(x -> each.accept(x.get()));
    }

    @Override
    public void forEach(Consumer<? super BLink<X>> action) {
        Object[] x = items.array();
        if (x.length > 0) {
            for (BLink a : ((BLink[]) x)) {
                if (a != null) {
                    BLink<X> b = a;
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
        int cLenMin1 = c.length - 1;
        while (true) {
            int i, j;
            if (right - left <= 7) {
                //bubble sort on a region of right less than 8?
                for (j = left + 1; j <= right; j++) {
                    BLink swap = c[j];
                    i = j - 1;
                    float swapV = pCmp(swap);
                    while (i >= left && cmpGT(c[i], swapV)) {
                        swap(c, i + 1, i);
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

                int median = (left + right) / 2;
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

                BLink temp = c[i];
                float tempV = pCmp(temp);

                while (true) {
                    while (i < cLenMin1 && cmpLT(c[++i], tempV)) ;
                    while (cmpGT(c[--j], tempV)) ;
                    if (j < i) {
                        break;
                    }
                    swap(c, j, i);
                }

                c[left + 1] = c[j];
                c[j] = temp;

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
        BLink swap = c[y];
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
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }


    @Override
    public float priMax() {
        BLink x = items.first();
        return x != null ? x.priSafe(-1) : 0f;
    }

    @Override
    public float priMin() {
        BLink x = items.last();
        return x != null ? x.priSafe(-1) : 0f;
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
