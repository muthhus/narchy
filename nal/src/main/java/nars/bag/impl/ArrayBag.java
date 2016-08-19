package nars.bag.impl;

import nars.$;
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
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * pending mass since last commit
     */
    float pending = 0;

    /**
     * mass as calculated in previous commit
     */
    private float mass = 0;

    public ArrayBag(@Deprecated int cap, BudgetMerge mergeFunction, Map<V, BLink<V>> map) {
        super(BLink[]::new, map);

        this.mergeFunction = mergeFunction;
        this.capacity = cap;
    }


    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }


    @Override
    protected void update(BLink<V> toAdd) {

        int s = size();
        int c = capacity();

        boolean forceClean = toAdd == null; //force clean if this is a commit and not an addition update

        int sizeThresh = forceClean ? 0 : c + ((toAdd!=null) ? 1 : 0);
        boolean modified = false;

        SortedArray<BLink<V>> items = this.items;
        if (s > sizeThresh) {

            List<BLink<V>> toRemove = $.newArrayList(s - sizeThresh);

            //first step: remove any nulls and deleted values
            s -= removeDeletedAtBottom(toRemove);

            //second step: if still not enough, do a hardcore removal of the lowest ranked items until quota is met
            removeWeakestUntilUnderCapacity(s, toRemove, toAdd != null);


            //do full removal outside of the synchronized phase, possibly interleaving with another thread doing the same thing
            for (int i = 0, toRemoveSize = toRemove.size(); i < toRemoveSize; i++) {
                BLink<V> w = toRemove.get(i);

                V k = w.get();
                BLink<V> k2 = map.remove(k);

                if (k2 != w) {
                    //throw new RuntimeException(
                    System.err.println("bag inconsistency: " + w + " removed but " + k2 + " may still be in the items list");
                    //reinsert it because it must have been added in the mean-time:
                    map.putIfAbsent(k, k2);
                }

                onRemoved(k, w);

                w.delete();

                modified = true;
            }
        }

        if (toAdd != null) {
            synchronized (items) {
                //the item key,value should already be in the map before reaching here
                items.add(toAdd, this);
                modified = true;
            }
        }

        if (modified)
            updateRange(); //regardless, this also handles case when policy changed and allowed more capacity which should cause minPri to go to -1

    }

    private void removeWeakestUntilUnderCapacity(int s, List<BLink<V>> toRemove, boolean pendingAddition) {
        SortedArray<BLink<V>> items = this.items;
        while (!isEmpty() && ((s - capacity()) + (pendingAddition ? 1 : 0)) > 0) {
            BLink<V> w;
            synchronized (items) {
                w = items.remove(size() - 1);
            }
            toRemove.add(w);
            s--;
        }
    }

    @Override
    public final int compare(@Nullable BLink o1, @Nullable BLink o2) {
        float f1 = cmp(o1);
        float f2 = cmp(o2);

        if (f1 < f2)
            return 1;           // Neither val is NaN, thisVal is smaller
        if (f1 > f2)
            return -1;            // Neither val is NaN, thisVal is larger
        return 0;
    }



    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(@Nullable BLink o1, @Nullable BLink o2) {
        return (cmp(o1) < cmp(o2));
    }
    static final boolean cmpGT(@Nullable BLink o1, float o2) {
        return (cmp(o1) < o2);
    }

    /**
     * true iff o1 > o2
     */
    static final boolean cmpGT(float o1PriElseNeg1, @Nullable BLink o2) {
        return (o1PriElseNeg1 < cmp(o2));
    }


    /**
     * true iff o1 < o2
     */
    static final boolean cmpLT(@Nullable BLink o1, @Nullable BLink o2) {
        return (cmp(o1) > cmp(o2));
    }
    static final boolean cmpLT(@Nullable BLink o1, float o2) {
        return (cmp(o1) > o2);
    }

    /** gets the scalar float value used in a comparison of BLink's */
    static float cmp(@Nullable Budgeted b) {
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
        //}
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
        if (bp != bp) { //already deleted
            return;
        }


        Insertion ii = new Insertion(this, b, scale, overflow);
        BLink<V> v = map.compute(key, ii);

        int r = ii.result;
        switch (r) {
            case 0:
                updateRange(); //in case the merged item determined the min priority
                break;
            case +1:
                update(v);
                onActive(key);
                break;
            case -1:
                onRemoved(key, null);
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

    @NotNull
    protected BLink<V> newLink(@NotNull V i, Budgeted b) {

        if (i instanceof Budgeted)
            return new StrongBLinkToBudgeted((Budgeted) i, b);
        else
            return new StrongBLink(i, b);
    }

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
            } else {
                float pending = this.pending;
                this.pending = 0; //reset pending accumulator

                float r = 1f - (existing / (existing + pending));

                a = (r >= Param.BUDGET_EPSILON) ? new Forget(r) : null;
            }

            commit(a);
        } else {
            minPriIfFull = -1;
        }

        return this;
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

        int s = size();
        if (s > 0) {
            synchronized (items) {
                int lowestUnsorted = updateExisting(each, s);

                if (lowestUnsorted != -1) {
                    qsort(new int[24 /* estimate */], items.array(), 0 /*dirtyStart - 1*/, s);
                } // else: perfectly sorted
            }

            update(null);
        }


        return this;
    }


    /**
     * value will be null if this is an event which was rejected on input
     */
    protected void onRemoved(@NotNull V key, @Nullable BLink<V> value) {

    }

    protected void onActive(V key) {

    }

    /**
     * if not full, this value must be set to -1
     */
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
        int i = s - 1;
        float weightedMass = 0;
        @NotNull BLink<V> beneath = l[i]; //compares with self below to avoid a null check in subsequent iterations
        for (; i >= 0; ) {
            BLink<V> b = l[i];

            if (b != null) {
                if (eachNotNull)
                    each.accept(b);

                b.commit();
            }

            float bCmp = cmp(b);
            if (bCmp > 0) {
                weightedMass += bCmp * b.dur();
            }

            if (lowestUnsorted == -1 && cmpGT(bCmp, beneath)) {
                lowestUnsorted = i + 1;
            }

            beneath = b;
            i--;
        }

        this.mass = weightedMass;

        return lowestUnsorted;
    }

    /**
     * must be called in synchronized(items) block
     */
    private int removeDeletedAtBottom(List<BLink<V>> removed) {


        SortedArray<BLink<V>> items = this.items;

        int i, j;
        BLink<V>[] l = items.array();
        j = i = Math.min(l.length, size()) - 1;

        int removedFromMap = 0;

        if (i > 0) {

            synchronized (items) {
                while ((i >= 0) && (l[i] == null)) {
                    i--;
                    removedFromMap++;
                }

                BLink<V> ii;
                while (i >= 0 && (ii = l[i]).isDeleted()) {

                    removed.add(ii);

                    l[i--] = null;

                    removedFromMap++;
                }

                if (i != j)
                    items._setSize(i + 1); //quickly remove null entries from the end by skipping past them
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
        int cap = capacity();
        float min = -1;
        if (s >= cap) {
            BLink<V>[] a = items.array();
            if (a.length >= cap - 1) {
                BLink<V> last = a[cap - 1];
                if (last != null) {
                    min = last.priIfFiniteElseNeg1();
                }
            } //else the array hasnt even grown large enough to reach the capacity so it is not full
        }

        this.minPriIfFull = min;

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
            int i, j;
            if (right - left <= 7) {
                BLink swap;
                //bubble sort on a region of size less than 8?
                for (j = left + 1; j <= right; j++) {
                    swap = c[j];
                    i = j - 1;
                    float swapV = cmp(swap);
                    while (i >= left && cmpGT(c[i], swapV)) {
                        BLink x = c[i];
                        c[i] = c[i + 1];
                        c[i + 1] = x;
                        i--;
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left =  stack[stack_pointer--];
                } else {
                    break;
                }
            } else {
                BLink swap;

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

                {
                    BLink temp = c[i];
                    float tempV = cmp(temp);

                    while (true) {
                        while (cmpLT(c[++i], tempV)) ;
                        while (cmpGT(c[--j], tempV)) ;
                        if (j < i) {
                            break;
                        }
                        swap = c[i];
                        c[i] = c[j];
                        c[j] = swap;
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
    static final class Insertion<V> implements BiFunction<V, BLink, BLink> {

        private ArrayBag arrayBag;
        @Nullable
        private final MutableFloat overflow;
        private final Budgeted b;

        /**
         * TODO this field can be re-used for 'activated' return value
         * -1 = deactivated, +1 = activated, 0 = no change
         */
        private final float scale;

        int result = 0;

        public Insertion(ArrayBag arrayBag, Budgeted b, float scale, @Nullable MutableFloat overflow) {
            this.arrayBag = arrayBag;
            this.b = b;
            this.scale = scale;
            this.overflow = overflow;
        }


        @Nullable
        @Override
        public BLink apply(@NotNull Object key, @Nullable BLink existing) {

            float scale = this.scale;
            Budgeted b = this.b;
            ArrayBag bag = this.arrayBag;

            if (existing != null) {

                if (existing == b) {
                    throw new RuntimeException("budget self merge");
                }

                //existing.isDeleted() /* if deleted, we will merge replacing it as if it were zero:

                float pBefore = existing.priNext() * existing.durNext();
                float o = bag.mergeFunction.merge(existing, b, scale);
                if (overflow != null)
                    overflow.add(o);

                bag.mass += existing.priNext() * existing.durNext() - pBefore;

                return existing;
            } else {

                BLink r;
                float bp = b.pri() * scale;
                if (bag.minPriIfFull > bp) {
                    //reject due to insufficient budget
                    bag.pending += bp * b.dur(); //include failed input in pending
                    this.result = -1;
                    r = null;
                } else {
                    //accepted for insert
                    BLink nvv = bag.newLink(key, b);
                    nvv.priMult(scale);

                    this.result = +1;
                    r = nvv;
                }
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
