package jcog.bag.impl;

import com.google.common.util.concurrent.AtomicDouble;
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
    public final AtomicDouble pressure = new AtomicDouble();

    public float mass;

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
        pressure.addAndGet(f);
    }

    @NotNull
    @Override
    public Iterator<PLink<X>> iterator() {
        ensureSorted();
        return super.iterator();
    }

    /**
     * returns true unless failed to add during 'add' operation or becomes empty
     */
    @Override
    protected boolean updateItems(@Nullable PLink<X> toAdd) {




        int c = capacity();
        List<PLink> pendingRemoval = null;
        boolean result;
        {
            int additional = (toAdd != null) ? 1 : 0;

            int s = size();

            int nextSize = s + additional;

            int needsRemoved = nextSize - c;
            if (needsRemoved > 0) {

                synchronized (items) {
                    pendingRemoval = new FasterList(needsRemoved);
                    clean(toAdd, s, nextSize - c, pendingRemoval);
                }


                pendingRemoval.forEach(this::onRemoved); //execute outside of synchronization


            }


            if (toAdd != null) {
                int ss = size();
                if (ss < c) {
                    synchronized (items) {
                        items.add(toAdd, this);
                    }
                    result = true;
                } else {
                    //throw new RuntimeException("list became full during insert");
                    result = false;
                }
            } else {
                result = size() > 0;
            }

        }

        return result;
    }

    private void clean(@Nullable PLink<X> toAdd, int s, int minRemoved, List<PLink> trash) {

        if (cleanDeletedEntries()) {
            //first step: remove any nulls and deleted values
            s -= removeDeleted(trash, minRemoved);
        }

        //second step: if still not enough, do a hardcore removal of the lowest ranked items until quota is met
        int s1 = s;
        SortedArray<PLink<X>> items1 = this.items;
        final int c = capacity;
        while (s1 > 0 && ((s1 - c) + (toAdd != null ? 1 : 0)) > 0) {
            PLink<X> w1 = items1.remove(s1 - 1);
            if (w1 != null) //skip over nulls
                trash.add(w1);
            s1--;
        }

        int trashed = trash.size();
        for (int i = 0; i < trashed; i++) {
            PLink<X> w = trash.get(i);
            map.remove(key(w));
        }
    }


    /**
     * return whether to clean deleted entries prior to removing any lowest ranked items
     */
    protected boolean cleanDeletedEntries() {
        return true;
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
    public Bag<X, PLink<X>> sample(@NotNull Bag.BagCursor<? super PLink<X>> each, boolean pop) {
        sample(each, 0, pop);
        return this;
    }

    /**
     * @param each
     * @param startingIndex if negative, a random starting location is used
     */
    protected void sample(@NotNull Bag.@NotNull BagCursor<? super PLink<X>> each, int startingIndex, boolean pop) {
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

            if (x != null/*.remove*/) {
                if (pop) {
                    x = remove(key(x));
                    if (x == null)
                        continue;
                    modified = true;
                }

                next = each.next(x);
                /*if (remove(key(x))!=null)
                    modified = true;*/
            }
        }

        if (modified) {
            commit(null);
        }
    }

    @Override
    public final PLink<X> put(@NotNull PLink<X> b, @Nullable MutableFloat overflow) {

        float[] incoming = { b.priSafe(0) };

        final boolean[] isNew = {false};

        X key = key(b);
        PLink<X> v = map.compute(key, (kk, existing) -> {
            PLink<X> res;

            if (existing != null) {
                //MERGE
                res = existing;
                float oo = existing!=b ? mergeFunction.merge(existing, b) : incoming[0] /* all of it if identical */;
                if (oo > 0) {
                    incoming[0] -= oo; //release any unabsorbed pressure
                    if (overflow != null) overflow.add(oo);
                }

            } else {
                //NEW

                if (size() >= capacity && incoming[0] < priMinFast(-1)) {
                    res = null; //failed insert
                } else {
                    isNew[0] = true;
                    res = b;
                }
            }

            return res;
        });

        pressurize(incoming[0]);

        if (v == null) {
            return null; //rejected
        }

        if (isNew[0]) {
            boolean added = updateItems(v); //attempt new insert
            if (!added) {
                map.remove(key);
                return null; //reject
            } else {
                onAdded(v);
            }

        } else {
            unsorted.set(true); //merging may have shifted ordering, so sort later
        }

        return v;

    }


    @Nullable
    @Override
    protected PLink<X> addItem(@NotNull PLink<X> i) {
        throw new UnsupportedOperationException();
    }


    @Override
    @Deprecated
    public Bag<X, PLink<X>> commit() {
        double p = this.pressure.getAndSet(0);
        if (p > 0) {
            return commit(PForget.forget(size(), capacity(), (float) p, mass, PForget.DEFAULT_TEMP, Priority.EPSILON, PForget::new));
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


        float mass = 0;
        //synchronized (items) {
        int iii = size();
        for (int i = 0; i < iii; i++) {
            PLink x = get(i);
            if (x != null)
                mass += x.priSafe(0);
        }
        //}
        this.mass = mass;


    }


    /**
     * applies the 'each' consumer and commit simultaneously, noting the range of items that will need sorted
     */
    @NotNull
    protected ArrayBag<X> update(@Nullable Consumer<PLink<X>> each, boolean checkCapacity) {
        boolean needsSort = false;

        synchronized (items) {
            if (size() > 0) {
                if (checkCapacity)
                    if (!updateItems(null))
                        return this;

                if (each != null) {
                    needsSort = !updateBudget(each);
                } else {
                    needsSort = false;
                }
            }
        }

        if (needsSort) {
            unsorted.set(true);
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
            int[] stack = new int[sortSize(s) /* estimate */];
            synchronized (items) {
                qsort(stack, items.array(), 0 /*dirtyStart - 1*/, (s - 1));
            }
        }
    }

    static int sortSize(int s) {
        //TODO get a better calculation; this is an estimate, probably some ~log2(size) relationship
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


    public @Nullable PLink<X> remove(boolean topOrBottom) {
        @Nullable PLink<X> x = topOrBottom ? top() : bottom();
        if (x != null) {
            remove(key(x));
            return x;
        }
        return null;
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

    /**
     * doesnt ensure sorting to avoid synchronization
     */
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
