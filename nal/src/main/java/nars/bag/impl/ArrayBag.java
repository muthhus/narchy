package nars.bag.impl;

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

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 */
public class ArrayBag<V> extends SortedListTable<V, BLink<V>> implements Bag<V>, BiFunction<RawBudget, RawBudget, RawBudget> {


    public final BudgetMerge mergeFunction;


    abstract public static class BagPendings<V> {
        abstract public void capacity(int c);

        abstract public void add(V v, float p, float d, float q);

        abstract public int size();

        abstract public void apply(ArrayBag<V> target);

        abstract public float mass(ArrayBag<V> bag);

        abstract public void clear();
    }


    final BagPendings<V> pending;

    public ArrayBag(int cap, BudgetMerge mergeFunction) {
        super(BLink[]::new,

                //new ConcurrentHashMapUnsafe<V, BLink<V>>(),
                //new LinkedHashMap<>(cap),
                new HashMap<>(cap)
                //Global.newHashMap(cap),

        );

        this.mergeFunction = mergeFunction;
        this.pending = newPendings();

        setCapacity(cap);
    }

    protected BagPendings<V> newPendings() {
        return new ListBagPendings(mergeFunction);
    }

    public void setCapacity(int newCapacity) {
        int current = capacity();
        if (current != newCapacity) {
            pending.capacity(newCapacity);
            super.setCapacity(newCapacity);
        }

    }

    @Override
    protected final void removeWeakest(Object reason) {
        synchronized(map) {
            if (!removeDeletedAtBottom()) {
                remove(weakest()).delete(reason);
            }
        }
    }

    @Override
    public final int compare(@NotNull BLink o1, @NotNull BLink o2) {
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
     * true iff o1 < o2
     */
    static final boolean cmpLT(@NotNull BLink o1, @NotNull BLink o2) {
        return (priIfFiniteElseNeg1(o1) > priIfFiniteElseNeg1(o2));
    }

    static float priIfFiniteElseNeg1(Budgeted b) {
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


    //    @Override public V put(V k, Budget b) {
//        //TODO use Map.compute.., etc
//
//        BagBudget<V> v = getBudget(k);
//
//        if (v!=null) {
//            v.set(b);
//            return k;
//        } else {
//            index.put(k, b);
//            return null;
//        }
//    }

    //    protected CurveMap newIndex() {
//        return new CurveMap(
//                //new HashMap(capacity)
//                Global.newHashMap(capacity()),
//                //new UnifiedMap(capacity)
//                //new CuckooMap(capacity)
//                items
//        );
//    }

    @Override
    public @Nullable BLink<V> sample() {
        throw new RuntimeException("unimpl");
    }

    @NotNull
    @Override
    public Bag<V> sample(int n, @NotNull Predicate<? super BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }

//    @NotNull
//    @Override
//    public Bag<V> filter(@NotNull Predicate<BLink> forEachIfFalseThenRemove) {
//
//        int n = items.size();
//        BLink<V>[] l = items.array();
//        if (n > 0) {
//            for (int i = 0; i < n; i++) {
//                BLink<V> h = l[i];
//                if (!forEachIfFalseThenRemove.test(h)) {
//                    removeKeyForValue(h); //only remove key, we remove the item here
//                    h.delete();
//                    items.remove(i--);
//                    n--;
//                }
//            }
//        }
//        return this;
//    }

    //    public void validate() {
//        int in = ArrayTable.this.size();
//        int is = items.size();
//        if (Math.abs(is - in) > 0) {
////                System.err.println("INDEX");
////                for (Object o : index.values()) {
////                    System.err.println(o);
////                }
////                System.err.println("ITEMS:");
////                for (Object o : items) {
////                    System.err.println(o);
////                }
//
//            Set<V> difference = Sets.symmetricDifference(
//                    new HashSet(((CollectorMap<V, L>) ArrayTable.this).values()),
//                    new HashSet(items)
//            );
//
//            System.err.println("DIFFERENCE");
//            for (Object o : difference) {
//                System.err.println("  " + o);
//            }
//
//            throw new RuntimeException("curvebag fault: " + in + " index, " + is + " items");
//        }
//
////            //test for a discrepency of +1/-1 difference between name and items
////            if ((is - in > 2) || (is - in < -2)) {
////                System.err.println(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
////                /*System.out.println(nameTable);
////                System.out.println(items);
////                if (is > in) {
////                    List<E> e = new ArrayList(items);
////                    for (E f : nameTable.values())
////                        e.remove(f);
////                    System.out.println("difference: " + e);
////                }*/
////                throw new RuntimeException(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
////            }
//    }

    //    /**
//     * Get an Item by key
//     *
//     * @param key The key of the Item
//     * @return The Item with the given key
//     */
//    @Override
//    public V get(K key) {
//        //TODO combine into one Map operation
//        V v = index.get(key);
//        if (v!=null && v.getBudget().isDeleted()) {
//            index.remove(key);
//            return null;
//        }
//        return v;
//    }


//    /**
//     * Choose an Item according to priority distribution and take it out of the
//     * Bag
//     *
//     * @return The selected Item, or null if this bag is empty
//     */
//    @Nullable
//    @Override
//    public BLink<V> pop() {
//        throw new UnsupportedOperationException();
//    }


    /**
     * Insert an item into the itemTable
     *
     * @param key The Item to put in
     * @return The updated budget
     */
    @Override
    public final void put(@NotNull V key, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflow) {

        if (b.isDeleted())
            throw new RuntimeException();

        BLink<V> existing = get(key);

        if (existing != null) {
            putExists(b, scale, existing, overflow);
        } else {
            if (isFull()) {
                synchronized(map) {
                    pending.add(key, b.pri() * scale, b.qua(), b.dur());
                }

            } else {
                putNewAndDeleteDisplaced(key, newLink(key, b.pri() * scale, b.qua(), b.dur()));
            }
        }
    }


    /**
     * the applied budget will not become effective until commit()
     */
    @NotNull
    private final void putExists(@NotNull Budgeted b, float scale, @NotNull BLink<V> existing, @Nullable MutableFloat overflow) {

        if (existing == b) {
            throw new RuntimeException("budget self merge");
        }

        float o = mergeFunction.merge(existing, b, scale);
        if (overflow != null)
            overflow.add(o);

    }

    @NotNull
    protected final BLink<V> newLink(@NotNull V i, @NotNull Budgeted b) {
        return newLink(i, b, 1f);
    }

    @NotNull
    protected final BLink<V> newLink(@NotNull V i, @NotNull Budgeted b, float scale) {
        return newLink(i, scale * b.pri(), b.dur(), b.qua());
    }

    @NotNull
    protected BLink<V> newLink(@NotNull V i, float p, float d, float q) {
        if (i instanceof Budgeted)
            return new StrongBLinkToBudgeted((Budgeted) i, p, d, q);
        else
            return new StrongBLink(i, p, d, q);
    }

    protected BLink<V> putNew(@NotNull V i, @NotNull BLink<V> newBudget) {
        synchronized(map) {
            BLink<V> removed = put(i, newBudget);
            return removed;
        }
    }


    @NotNull
    @Override
    @Deprecated
    public Bag<V> commit() {
        return commit(null);
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
                    int[] qsortStack = new int[16];
                    qsort(qsortStack, items.array(), 0 /*dirtyStart - 1*/, s);
                } // else: perfectly sorted

                removeDeletedAtBottom();
            }
        }

        synchronized (map) {
            BagPendings<V> p = this.pending;
            if (p != null) {
                this.bottomPri = bottomPriIfFull();
                p.apply(this);
            }
        }

        return this;
    }

    transient private float bottomPri;

    public final void commitPending(V key, float p, float d, float q) {
        //final BiConsumer<V,RawBudget> eachPending = (key, inc) -> {
        //            if (key == null)
//                continue; //link was destroyed before it could be processed

            /*BLink<V> existing = get(key);
            if (existing != null) {
                //SHOULD NOT BE IN THE MAP ALREADY
                mergeFunction.merge(existing, inc, 1f);
            }
            else {*/

        float v = bottomPri;
        if (v <= p) {
            putNewAndDeleteDisplaced(key, newLink(key, p, d, q));
            bottomPri = bottomPriIfFull();
        } else {
            putFail(key);
        }
                /*else {
                    insufficient, try again next commit
                }*/
        //}

    }

    /** wraps the putNew call with a suffix that destroys the link at the end */
    private final void putNewAndDeleteDisplaced(V key, @Nullable BLink<V> value) {
        BLink<V> displaced = putNew(key, value);
        if (displaced!=null)
            displaced.delete();
    }

    protected void putFail(V key) {

    }

    private final float bottomPriIfFull() {
        int s = size();
        return (s == capacity()) ? get(s - 1).pri() : -1f;
    }

    /**
     * returns the index of the lowest unsorted item
     */
    private int updateExisting(@Nullable Consumer<BLink> each, int s) {
//        int dirtyStart = -1;
        int lowestUnsorted = -1;

        final boolean eachNotNull = each != null;

        BLink<V>[] l = items.array();
        int t = s - 1;
        @NotNull BLink<V> beneath = l[t]; //compares with self below to avoid a null check in subsequent iterations
        for (int i = t; i >= 0; i--) {
            BLink<V> b = l[i];

            if (eachNotNull)
                each.accept(b);

            b.commit();


            if (lowestUnsorted == -1 && cmpGT(b, beneath)) {
                lowestUnsorted = i + 1;
            }

            beneath = b;
        }
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


    @Override
    public RawBudget apply(RawBudget bExisting, RawBudget bNext) {
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
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }


    @Override
    public void clear() {
        pending.clear();
        super.clear();
    }


    @Override
    public float priMax() {
        return isEmpty() ? 0 : items.first().pri();
    }

    @Override
    public float priMin() {
        return isEmpty() ? 0 : items.last().pri();
    }

    /**
     * returns mass (index 0) pending mass (index 1)
     */
    public float[] preCommit() {
        BLink<V>[] l = items.array();
        int s = size();
        float mass = 0, pendingMass = 0;
        for (int i = s - 1; i >= 0; i--) {
            BLink<V> b = l[i];
            float d = b.dur(); //HACK ignores any pending durDelta change
            mass += d * b.priIfFiniteElseZero();
            pendingMass += d * b.priDelta();
        }
        pendingMass += pending.mass(this);
        return new float[]{mass, pendingMass};
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
