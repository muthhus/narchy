package jcog.bag.impl;

import jcog.bag.Bag;
import jcog.data.sorted.SortedArray;
import jcog.list.FasterList;
import jcog.pri.*;
import jcog.table.SortedListTable;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Consumer;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 * TODO extract a version of this which will work for any Prioritized, not only BLink
 */
public class ArrayBag<X> extends SortedListTable<X, PLink<X>> implements Bag<X, PLink<X>> {

    public final PriMerge mergeFunction;


    /**
     * inbound pressure sum since last commit
     */
    public final DoubleAdder pressure = new DoubleAdder();

    float mass;

    private final AtomicBoolean unsorted = new AtomicBoolean(false);

    public ArrayBag(PriMerge mergeFunction, @NotNull Map<X, PLink<X>> map) {
        this(0, mergeFunction, map);
    }

    static final class SortedPLinks extends SortedArray {
        @Override
        protected Object[] newArray(int oldSize) {
            return new PLink[grow(oldSize)];
        }
    }

    public ArrayBag(@Deprecated int cap, PriMerge mergeFunction, @NotNull Map<X, PLink<X>> map) {
        super(new SortedPLinks(), map);

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
        pressure.add(f);
    }

    @NotNull
    @Override
    public Iterator<PLink<X>> iterator() {
        ensureSorted();
        return super.iterator();
    }

    /**
     * returns true unless failed to add during 'add' operation or is empty
     */
    @Override
    protected boolean updateItems(@Nullable PLink<X> toAdd) {


        SortedArray<PLink<X>> items;

        List<PLink> pendingRemoval = null;
        boolean result;
        synchronized (items = this.items) {
            int additional = (toAdd != null) ? 1 : 0;
            int c = capacity();

            int s = size();

            int nextSize = s + additional;

            if (nextSize > c) {
                pendingRemoval = new FasterList(nextSize - c);
                s = clean(toAdd, s, nextSize - c, pendingRemoval);
                clean2(pendingRemoval);
                if (s + additional > c) {
                    return false; //throw new RuntimeException("overflow");
                }
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

    private int clean(@Nullable PLink<X> toAdd, int s, int minRemoved, List<PLink> trash) {

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
        return true;
    }

    private void clean2(List<PLink> trash) {
        int toRemoveSize = trash.size();

        for (int i = 0; i < toRemoveSize; i++) {
            PLink<X> w = trash.get(i);

            map.remove(key(w));

            onRemoved(w);

        }


    }

    private int removeWeakestUntilUnderCapacity(int s, @NotNull List<PLink> toRemove, boolean pendingAddition) {
        SortedArray<PLink<X>> items = this.items;
        final int c = capacity;
        while (!isEmpty() && ((s - c) + (pendingAddition ? 1 : 0)) > 0) {
            PLink<X> w = items.remove(s - 1);
            if (w != null) //skip over nulls
                toRemove.add(w);
            s--;
        }
        return s;
    }


    @Override
    public final float floatValueOf(PLink x) {
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
    static boolean cmpGT(@Nullable PLink o1, @Nullable PLink o2) {
        return cmpGT(o1, pCmp(o2));
    }

    static boolean cmpGT(@Nullable PLink o1, float o2) {
        return (pCmp(o1) < o2);
    }

    /**
     * true iff o1 > o2
     */
    static boolean cmpGT(float o1, @Nullable PLink o2) {
        return (o1 < pCmp(o2));
    }


    /**
     * true iff o1 < o2
     */
    static boolean cmpLT(@Nullable PLink o1, @Nullable PLink o2) {
        return cmpLT(o1, pCmp(o2));
    }

    static boolean cmpLT(@Nullable PLink o1, float o2) {
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
    @NotNull
    public final X key(@NotNull PLink<X> l) {
        return l.get();
    }


    /**
     * iterates in sorted order, descending
     */
    @NotNull
    @Override
    public Bag<X, PLink<X>> sample(@NotNull Bag.BagCursor<? super PLink<X>> each) {
        sample(each, 0);
        return this;
    }

    /**
     *
     * @param each
     * @param startingIndex if negative, a random starting location is used
     */
    protected void sample(@NotNull Bag.@NotNull BagCursor<? super PLink<X>> each, int startingIndex) {
        int i = startingIndex;
        if (i < 0) {
            int s = size();
            if (s == 0) return;
            else if (s == 1) i = 0;
            else i = ThreadLocalRandom.current().nextInt(s);
        }

        boolean modified = false;
        BagCursorAction next = BagCursorAction.Next;
        int s;
        while (!next.stop && (0 < (s = size()))) {
            if (i >= s) i = 0;
            PLink<X> x = get(i++);

            if (x != null && (next = each.next(x)).remove) {
                if (remove(key(x))!=null)
                    modified = true;
            }
        }

        if (modified) {
            commit(null);
        }
    }

    @Override
    public final PLink<X> put(@NotNull PLink<X> b, float scale, @Nullable MutableFloat overflow) {

        pressurize(b.priSafe(0) * scale);

        final boolean[] isNew = {false};

        X key = key(b);
        PLink<X> v = map.compute(key, (kk, existing) -> {
            PLink<X> res;
            float o;
            if (existing != null) {
                //merge
                res = existing;

                o = mergeFunction.merge(existing, b, scale);

            } else {
                //new
                PLink<X> n = new RawPLink<>(b.get(), 0);
                float oo = mergeFunction.merge(n, b, scale);
                float np = n.pri();

                if (size() >= capacity && np < priMinFast(-1)) {
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
            boolean added = updateItems(v); //attempt new insert
            if (added) {
                onAdded(v);
                return v;
            }

            map.remove(key);
            return null; //reject

        } else {
            unsorted.set(true);
            return v;
        }

    }


    @Nullable
    @Override
    protected PLink<X> addItem(@NotNull PLink<X> i) {
        throw new UnsupportedOperationException();
    }


    @Override
    @Deprecated
    public Bag<X, PLink<X>> commit() {
        double p = this.pressure.sumThenReset();
        if (p > 0) {
            return commit(PForget.forget(size(), capacity(), (float)p, mass, PForget.DEFAULT_TEMP, Priority.EPSILON, PForget::new));
        }
        return this;
    }

    @Override
    @NotNull
    public final ArrayBag<X> commit(Consumer<PLink<X>> update) {
        commit(update, false);
        return this;
    }

    private void commit(@Nullable Consumer<PLink<X>> update, boolean checkCapacity) {

        if (update != null || checkCapacity)
            update(update, checkCapacity);

        if (update != null) {
            float mass = 0;
            synchronized (items) {
                int iii = size();
                for (int i = 0; i < iii; i++) {
                    PLink x = get(i);
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
    protected ArrayBag<X> update(@Nullable Consumer<PLink<X>> each, boolean checkCapacity) {

        synchronized (items) {

            if (size() > 0) {
                if (checkCapacity)
                    if (!updateItems(null))
                        return this;

                boolean needsSort;
                if (each != null) {
                    needsSort = !updateBudget(each);
                } else {
                    needsSort = false;
                }


                if (needsSort) {
                    unsorted.set(true);
                }

            }

        }

        return this;
    }


//    protected void sortAfterUpdate() {
//        unsorted.set(true);
//        sort();
//    }

    protected void sort() {
        int s = size();
        if (s > 1) {
            synchronized (items) {
                qsort(new int[sortSize(s) /* estimate */], items.array(), 0 /*dirtyStart - 1*/, (s - 1));
            }
        }
    }

    static int sortSize(int s) {
        //estimate, probably some ~log2(size) relationship
        if (s < 16)
            return 4;
        if (s < 64)
            return 6;
        if (s < 128)
            return 8;
        if (s < 2048)
            return 16;

        return 32;
    }

    /**
     * returns whether the items list was detected to be sorted
     */
    private boolean updateBudget(@NotNull Consumer<PLink<X>> each) {
//        int dirtyStart = -1;
        boolean sorted = true;


        int s = size();
        PLink[] l = items.array();
        //@NotNull PLink<V> beneath = l[i]; //compares with self below to avoid a null check in subsequent iterations
        float pBelow = -2;
        for (int i = s - 1; i >= 0; ) {
            PLink b = l[i];

            float p = (b != null) ? b.priSafe(-2) : -2; //sort nulls to the end of the end

            if (p >= 0) { //Bag.active(p) semantics
                each.accept(b);
            }

            if (pBelow - b.priSafe(-2) >= Priority.EPSILON) {
                sorted = false;
            }

            pBelow = p;
            i--;
        }


        return sorted;
    }


    private int removeDeleted(@NotNull List<PLink> trash, int minRemoved) {

        SortedArray<PLink<X>> items = this.items;
        final Object[] l = items.array();
        int removedFromMap = 0;

        //iterate in reverse since null entries should be more likely to gather at the end
        for (int s = size() - 1; removedFromMap < minRemoved && s >= 0; s--) {
            PLink x = (PLink) l[s];
            if (x == null || (x.isDeleted() && trash.add(x))) {
                items.removeFast(s);
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
    public float pri(@NotNull PLink<X> key) {
        return key.pri();
        //throw new UnsupportedOperationException("TODO currently this bag works with PLink.pri() directly");
    }

    @Override
    public void forEach(int max, @NotNull Consumer<? super PLink<X>> action) {
        ensureSorted();

        Object[] x = items.array();
        if (x.length > 0) {
            for (PLink a : ((PLink[]) x)) {
                if (a != null) {
                    PLink<X> b = a;
                    float p = b.pri();
                    if (p == p) {
                        action.accept(b);
                        if (--max <= 0)
                            break;
                    }
                }

            }
        }

    }

    @Override
    public void forEachKey(@NotNull Consumer<? super X> each) {
        forEach(x -> each.accept(x.get()));
    }

    @Override
    public void forEach(Consumer<? super PLink<X>> action) {

        forEach(Integer.MAX_VALUE, action);
    }


    //    public void sortPartial(float sortPercentage) {
//        int s = size();
//        int sortRange = (int) Math.ceil(s * sortPercentage);
//        int start = sampleIndex();
//        int end = Math.min(start + sortRange, s - 1);
//
//        qsort(new int[sortSize(sortRange)], items.array(), start, end);
//    }

    /**
     * http://kosbie.net/cmu/summer-08/15-100/handouts/IterativeQuickSort.java
     */

    public static void qsort(int[] stack, PLink[] c, int left, int right) {
        int stack_pointer = -1;
        int cLenMin1 = c.length - 1;
        while (true) {
            int i, j;
            if (right - left <= 7) {
                //bubble sort on a region of right less than 8?
                for (j = left + 1; j <= right; j++) {
                    PLink swap = c[j];
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
                PLink swap;

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

                PLink temp = c[i];
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

    public static void swap(PLink[] c, int x, int y) {
        PLink swap = c[y];
        c[y] = c[x];
        c[x] = swap;
    }

    //    final Comparator<? super PLink<V>> comparator = (a, b) -> {
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

    protected void ensureSorted() {
        if (unsorted.compareAndSet(true, false)) {
            sort();
        }
    }

    @Override
    public float priMax() {
        PLink x;
        ensureSorted();
        x = items.first();
        return x != null ? x.priSafe(0) : 0f;
    }

    @Override
    public float priMin() {
        ensureSorted();
        return priMinFast(0);
    }

    /** doesnt ensure sorting to avoid synchronization */
    float priMinFast(float ifDeleted) {
        PLink x;
        x = items.last();
        return x != null ? x.priSafe(ifDeleted) : ifDeleted;
    }


//    public final void popAll(@NotNull Consumer<PLink<V>> receiver) {
//        forEach(receiver);
//        clear();
//    }

//    public void pop(@NotNull Consumer<PLink<V>> receiver, int n) {
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
//                PLink<V> x = items.remove(dirtyStart); //remove directly from the decorated list
//                items.add(x); //add using the sorted list
//
//            } else if ( dirtyRange < Math.max(1, reinsertionThreshold * s) ) {
//                //Special case: a limited number of unordered items
//                PLink<V>[] tmp = new BLink[dirtyRange];
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
