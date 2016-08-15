package nars.bag.impl;

import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import nars.Param;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.link.StrongBLink;
import nars.link.StrongBLinkToBudgeted;
import nars.util.data.sorted.SortedArray;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * pending mass since last commit
     */
    float pending = 0;

    /**
     * mass as calculated in previous commit
     */
    private float mass = 0;

    public ArrayBag(int cap, BudgetMerge mergeFunction, Map<V, BLink<V>> map) {
        super(BLink[]::new, map);

        this.mergeFunction = mergeFunction;

        setCapacity(cap);
    }


    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    protected final void removeWeakest(Object reason) {
        synchronized (map) {
            if (!removeDeletedAtBottom()) {
                @NotNull V w = weakest();
                if (w != null) {
                    BLink<V> ww = remove(w);
                    if (ww != null)
                        ww.delete(reason);
                }
            }
        }
    }

    @Nullable
    @Override
    public BLink<V> remove(@NotNull V x) {
        synchronized (map) {
            return super.remove(x);
        }
    }


    @Override
    public final int compare(@Nullable BLink o1, @Nullable BLink o2) {
        float f1 = priIfFiniteElseNeg1(o1);
        float f2 = priIfFiniteElseNeg1(o2);

        if (f1 < f2)
            return 1;           // Neither val is NaN, thisVal is smaller
        if (f1 > f2)
            return -1;            // Neither val is NaN, thisVal is larger
        return 0;
    }

    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(@NotNull BLink o1, @NotNull BLink o2) {
        return (priIfFiniteElseNeg1(o1) < priIfFiniteElseNeg1(o2));
    }

    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(float o1PriElseNeg1, @NotNull BLink o2) {
        return (o1PriElseNeg1 < priIfFiniteElseNeg1(o2));
    }

    /**
     * true iff o1 < o2
     */
    static final boolean cmpLT(@NotNull BLink o1, @NotNull BLink o2) {
        return (priIfFiniteElseNeg1(o1) > priIfFiniteElseNeg1(o2));
    }

    static float priIfFiniteElseNeg1(@Nullable Budgeted b) {
        if (b == null) return -1;
        float p = b.pri();
        return p == p ? p : -1;
        //return (b!=null) ? b.priIfFiniteElseNeg1() : -1f;
        //return b.priIfFiniteElseNeg1();
    }


    @Override
    public final V key(@NotNull BLink<V> l) {
        return l.get();
    }



    @Override
    public @Nullable BLink<V> sample() {
        throw new RuntimeException("unimpl");
    }

    @NotNull
    @Override
    public Bag<V> sample(int n, @NotNull Predicate<? super BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }


    @Override
    public void put(@NotNull ObjectFloatHashMap<? extends V> values, @NotNull Budgeted in,/*, MutableFloat overflow*/float scale, MutableFloat overflow) {

//        if (values.isEmpty())
//            return;

        ObjectFloatProcedure<V> p = (k, v) -> {
            put(k, in, v * scale, overflow);
        };

        //synchronized(map) {
            values.forEachKeyValue(p);
       // }
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

        float bp = b.pri();
        if (bp != bp) { //deleted
            return;
        }

        Insertion ii = new Insertion(this, b, scale, overflow);
        synchronized (map) {
            compute(key, ii);
        }

        BLink<V> dd = ii.displaced;
        if (dd!=null) {
            removeKeyForValue(dd);
            dd.delete();
        }

        if (ii.activated < 0) {
            putFail(key);
        }

        if (ii.activated > 0) {
            putActive(key);
        }

    }

    /**
     * the applied budget will not become effective until commit()
     */
    @NotNull
    protected final void putExists(@NotNull Budgeted b, @NotNull BLink<V> existing, @Nullable MutableFloat overflow) {

        if (existing == b) {
            throw new RuntimeException("budget self merge");
        }

        float pBefore = existing.priNext() * existing.durNext();
        float o = mergeFunction.merge(existing, b, 1f);
        if (overflow != null)
            overflow.add(o);

        this.mass += existing.priNext() * existing.durNext() - pBefore;

    }

//    @NotNull
//    protected final BLink<V> newLink(@NotNull V i, @NotNull Budgeted b) {
//        return newLink(i, b, 1f);
//    }

//    @NotNull
//    protected final BLink<V> newLink(@NotNull V i, @NotNull Budgeted b, float scale) {
//        return newLink(i, scale * b.pri(), b.dur(), b.qua());
//    }

    @NotNull
    protected BLink<V> newLink(@NotNull V i, float p, float d, float q) {
        if (i instanceof Budgeted)
            return new StrongBLinkToBudgeted((Budgeted) i, p, d, q);
        else
            return new StrongBLink(i, p, d, q);
    }

    protected @Nullable BLink<V> prePutNew(@NotNull V k, @NotNull BLink<V> v) {
        return mergeList(k, v, null);
    }

    @Nullable
    @Override
    protected BLink<V> addItem(BLink<V> x) {
        BLink<V> y = super.addItem(x);
        if (y!=x) {
            int s = size();
            if (s == capacity())
                updateRange(s);
        }
        return y;
    }

    @NotNull
    @Override
    public final synchronized Bag<V> commit() {

        if (!isEmpty()) {
            commit(autoforget());
        }


        return this;
    }

    private @Nullable Consumer<BLink> autoforget() {

        float existing = this.mass;
        if (existing == 0)
            return null; //nothing to forget

        float pending = this.pending;
        this.pending = 0; //reset pending accumulator

        float r = 1f - (existing / (existing + pending));


        return r >= Param.BUDGET_EPSILON ? new Forget(r) : null;
    }

    private static final class Forget implements Consumer<BLink> {
        public final float r;

        static final float maxEffectiveDurability = 1f;

        public Forget(float r) {
            this.r = r;
        }

        @Override
        public void accept(@NotNull BLink bLink) {
            float p = bLink.pri();
            if (p == p) {
                float d = bLink.dur();
                //float d = or(bLink.dur(), bLink.qua());
                //float d = Math.max(bLink.dur(), bLink.qua());
                bLink.setPriority(p * (1f - (r * (1f - d * maxEffectiveDurability))));
            }
        }

//        public Consumer<BLink> set(float r) {
//            this.r = r;
//            return this;
//        }

    }


    /**
     * applies the 'each' consumer and commit simultaneously, noting the range of items that will need sorted
     */
    @NotNull
    @Override
    public Bag<V> commit(@Nullable Consumer<BLink> each) {

        synchronized (map) {


            int s = size();
            if (s > 0) {
                int lowestUnsorted = updateExisting(each, s);

                if (lowestUnsorted != -1) {
                    int[] qsortStack = new int[24];
                    qsort(qsortStack, items.array(), 0 /*dirtyStart - 1*/, s);
                } // else: perfectly sorted

                removeDeletedAtBottom();
            }


        }


        return this;
    }

    /**
     * wraps the putNew call with a suffix that destroys the link at the end
     */
    protected final BLink<V> putNewAndReturnDisplaced(@NotNull V key, @Nullable BLink<V> value) {
        BLink<V> displaced = prePutNew(key, value);
        if (displaced!=value) {
            float dp = value.pri() * value.dur();

            if (displaced != null) {
                dp -= displaced.priIfFiniteElseZero() * displaced.dur();
            }

            this.mass += dp;
        }
        return displaced;
    }

    protected void putFail(V key) {

    }
    protected void putActive(V key) {

    }

    /** if not full, this value must be set to -1 */
    float minPriIfFull = -1;

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

        final boolean eachNotNull = each != null;

        BLink<V>[] l = items.array();
        int t = s - 1;
        float weightedMass = 0;
        @NotNull BLink<V> beneath = l[t]; //compares with self below to avoid a null check in subsequent iterations
        for (int i = t; i >= 0; i--) {
            BLink<V> b = l[i];

            if (eachNotNull)
                each.accept(b);

            b.commit();

            float o1PriElseNeg1 = priIfFiniteElseNeg1(b);
            if (o1PriElseNeg1 > 0) {
                weightedMass += o1PriElseNeg1 * b.dur();
            }

            if (lowestUnsorted == -1 && cmpGT(o1PriElseNeg1, beneath)) {
                lowestUnsorted = i + 1;
            }

            beneath = b;
        }

        this.mass = weightedMass;

        return lowestUnsorted;
    }

    private boolean removeDeletedAtBottom() {
        //remove deleted items they will collect at the end
        int i = size() - 1;
        BLink<V> ii;
        SortedArray<BLink<V>> items = this.items;
        BLink<V>[] l = items.array();

        int removed = 0;
        int toRemoveFromMap = 0;
        while (i > 0 && ((ii = l[i]) == null || (ii.isDeleted()))) {
            if (ii != null && removeKeyForValue(ii) == null) {
                //
                //throw new RuntimeException("Bag fault while trying to remove key by item value");

                //exhaustive removal, since the BLink has lost its key
                toRemoveFromMap++;
            } else {
                removed++;
            }

            //remove by known index rather than have to search for it by key or something
            //different from removeItem which also removes the key, but we have already done that above
            items.remove(i);
            i--;
        }

        updateRange(i);


        if (toRemoveFromMap > 0) {
//            int sizeBefore = map.size();
            if (map.values().removeIf(BLink::isDeleted)) {
                return true;
            }


            //EXTRA checks but which dont apply if dealing with weak links becaues they can get removed in the middle of checking them (heisenbug):
//            int currentSize = map.size();
//            if (sizeBefore - currentSize < toRemoveFromMap)
//                throw new RuntimeException("bag fault");
//            if (currentSize!=items.size())
//                throw new RuntimeException("bag fault");
        }


        return removed > 0;
    }

    private void updateRange(int size) {
        minPriIfFull = (size == capacity()) ? items.last().pri() : -1f;
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


    /**
     * http://kosbie.net/cmu/summer-08/15-100/handouts/IterativeQuickSort.java
     */

    public static void qsort(int[] stack, BLink[] c, int start, int size) {
        int left = start, right = size - 1, stack_pointer = -1;
        while (true) {
            int i;
            int j;
            BLink swap;
            if (right - left <= 7) {
                //bubble sort on a region of size less than 8?
                for (j = left + 1; j <= right; j++) {
                    swap = c[j];
                    i = j - 1;
                    while (i >= left && cmpGT(c[i], swap)) {
                        BLink x = c[i];
                        c[i] = c[i + 1];
                        c[i + 1] = x;
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
                int median = (left + right) >> 1;
                i = left + 1;
                j = right;
                swap = c[median];
                c[median] = c[i];
                c[i] = swap;
                if (cmpGT(c[left], c[right])) {
                    swap = c[left];
                    c[left] = c[right];
                    c[right] = swap;
                }
                if (cmpGT(c[i], c[right])) {
                    swap = c[i];
                    c[i] = c[right];
                    c[right] = swap;
                }
                if (cmpGT(c[left], c[i])) {
                    swap = c[left];
                    c[left] = c[i];
                    c[i] = swap;
                }
                BLink temp = c[i];

                while (true) {
                    while (cmpLT(c[++i], temp)) ;
                    while (cmpGT(c[--j], temp)) ;
                    if (j < i) {
                        break;
                    }
                    swap = c[i];
                    c[i] = c[j];
                    c[j] = swap;
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
        synchronized (map) {
            return super.toString() + '{' + items.getClass().getSimpleName() + '}';
        }
    }


    @Override
    public void clear() {
        synchronized (map) {
            super.clear();
        }
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
    static final class Insertion<V> implements BiFunction<V, BLink<V>, BLink<V>> {

        private ArrayBag<V> arrayBag;
        private final MutableFloat overflow;
        private final Budgeted b;

        private final float scale;

        /**
         * -1 = deactivated, +1 = activated, 0 = no change
         */
        int activated = 0;

        BLink<V> displaced = null;

        public Insertion(ArrayBag arrayBag, Budgeted b, float scale, @Nullable MutableFloat overflow) {
            this.arrayBag = arrayBag;
            this.b = b;
            this.scale = scale;
            this.overflow = overflow;
        }

        private BLink<V> newLink(V key) {
            return arrayBag.newLink(key, b.pri() * scale, b.qua(), b.dur());
        }

        @Override
        public BLink<V> apply(V key, BLink<V> existing) {


            if (existing != null) {
                arrayBag.putExists(newLink(key), existing, overflow);
                return existing;
            } else {
                BLink d, r;
                float bp = b.pri() * scale;
                int activated;
                if (arrayBag.minPriIfFull > bp) {
                    //insufficient budget
                    arrayBag.pending += bp * b.dur(); //include failed input in pending
                    d = null;
                    activated = -1;
                    r = null;
                } else {
                    //successfully displaced another item
                    BLink nvv = newLink(key);
                    d = arrayBag.putNewAndReturnDisplaced(key, nvv);
                    if (d != nvv) {
                        activated = +1;
                        r = nvv;
                    } else {
                        activated = -1;
                        r = null;
                    }
                }
                this.activated = activated;
                displaced = d;
                return r;
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
