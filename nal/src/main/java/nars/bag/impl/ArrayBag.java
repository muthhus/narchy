package nars.bag.impl;

import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.util.FastQuickSort;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.happy.collections.lists.decorators.SortedList_1x4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 */
public class ArrayBag<V> extends SortedArrayTable<V, BLink<V>> implements Bag<V> {

    /** this default value must be changed */
    @NotNull protected BudgetMerge mergeFunction = BudgetMerge.nullMerge;

    private final float reinsertionThreshold = 0.01f;

    public ArrayBag(int cap) {
        this(new FasterList(cap), new HashMap(cap));
        setCapacity(cap);
    }

    protected ArrayBag(@NotNull List<BLink<V>> items, Map<V, BLink<V>> map) {
        super(items, map);
    }

    @Override
    protected void removeWeakest(Object reason) {
        remove(weakest()).delete(reason);
    }

    @Override
    public final int compare(@NotNull BLink o1, @NotNull BLink o2) {
        float f1 = o1.priIfFiniteElseZero();
        float f2 = o2.priIfFiniteElseZero();
        if (f1 < f2)
            return 1;           // Neither val is NaN, thisVal is smaller
        if (f1 > f2)
            return -1;            // Neither val is NaN, thisVal is larger
        return 0;
    }

    static final int cmp(@NotNull BLink o1, @NotNull BLink o2) {
        float f1 = o1.priIfFiniteElseZero();
        float f2 = o2.priIfFiniteElseZero();
        if (f1 < f2)
            return 1;           // Neither val is NaN, thisVal is smaller
        if (f1 > f2)
            return -1;            // Neither val is NaN, thisVal is larger
        return 0;
    }

    @NotNull
    public Bag<V> merge(@NotNull BudgetMerge mergeFunction) {
        this.mergeFunction = mergeFunction;
        return this;
    }

    @Override
    public final V key(@NotNull BLink<V> l) {
        return l.get();
    }


    /**
     * returns amount overflowed
     */
    protected final float merge(@NotNull BLink<V> target, @NotNull Budgeted incoming, float scale) {
        return mergeFunction.merge(target, incoming, scale);
        /*if (overflow > 0)
            target.charge(overflow);*/
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

    @Nullable
    @Override
    public BLink<V> sample() {
        throw new RuntimeException("unimpl");
    }

    @NotNull
    @Override
    public Bag<V> sample(int n, @NotNull Consumer<? super BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }

    @NotNull
    @Override
    public Bag<V> filter(@NotNull Predicate<BLink> forEachIfFalseThenRemove) {
        List<BLink<V>> l = items;
        int n = l.size();
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                BLink<V> h = l.get(i);
                if (!forEachIfFalseThenRemove.test(h)) {
                    removeKeyForValue(h); //only remove key, we remove the item here
                    h.delete();
                    l.remove(i--);
                    n--;
                }
            }
        }
        return this;
    }

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
     * @param i The Item to put in
     * @return The updated budget
     */
    @Nullable
    @Override
    public final BLink<V> put(@NotNull V i, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflow) {

        BLink<V> existing = get(i);

        return (existing != null) ?
                putExists(b, scale, existing, overflow) :
                putNew(i, link(i, b, scale));

//        //TODO optional displacement until next update, allowing sub-threshold to grow beyond threshold
//        BagBudget<V> displaced = null;
//        if (full()) {
//            if (getPriorityMin() > b.getPriority()) {
//                //insufficient priority to enter the bag
//                //remove the key which was put() at beginning of this method
//                return index.removeKey(i);
//            }
//            displaced = removeLowest();
//        }
        //now that room is available:

    }

    /**
     * the applied budget will not become effective until commit()
     */
    @NotNull
    private final BLink<V> putExists(@NotNull Budgeted b, float scale, @NotNull BLink<V> existing, @Nullable MutableFloat overflow) {

        if (existing != b) {

            float o = merge(existing, b, scale);
            if (overflow != null)
                overflow.add(o);

        }

        return existing;
    }

    protected BLink<V> link(V i, Budgeted b, float scale) {
        if (b instanceof BLink)
            return (BLink)b;
        if (i instanceof BLink)
            return (BLink)i;
        return new BLink<>(i, b, scale);
    }

    @Nullable
    protected BLink<V> putNew(V i, BLink<V> newBudget) {
        newBudget.commit(); //?? necessary
        return put(i, newBudget);
    }


//    @Nullable
//    @Override
//    public Bag<V> commit() {
//
//        forEach(BLink::commit);
//        ((FasterList)items.list).sortThis(this);
//
//        return this;
//    }
    @NotNull
    @Override
    @Deprecated public Bag<V> commit() {
//        super.commit();
//        sampler.commit(this);
//        return this;
        return commit(n -> {});
    }

    /** applies the 'each' consumer and commit simultaneously, noting the range of items that will need sorted */
    @Override public Bag<V> commit(@NotNull Consumer<BLink> each) {
        int s = size();
        if (s == 0)
            return this;

        int dirtyStart = -1;
        int dirtyEnd = -1;
        @NotNull BLink<V> prev = item(0); //compares with self below to avoid a null check in subsequent iterations
        for (int i = 0; i < s; i++) {
            BLink<V> b = item(i);

            each.accept(b);

            if (b.commit() && compare(b, prev) < 0) {
                //detected out-of-order

                if (dirtyStart == -1) {
                    dirtyStart = i; //TODO this only happens once
                }

                dirtyEnd = i;
            }

            prev = b;
        }

        if (dirtyStart == -1) {
            //already sorted
            return this;
        } else {

            //Special case: only one unordered item; remove and reinsert
            int dirtyRange = 1 + dirtyEnd - dirtyStart;
            SortedList_1x4<BLink<V>> items = this.items;
            List<BLink<V>> itemList = items.list;

            if (dirtyRange == 1) {
                //TODO
                BLink<V> x = itemList.remove(dirtyStart); //remove directly from the decorated list
                items.add(x); //add using the sorted list
                return this;
            } else if ( dirtyRange < Math.max(1, reinsertionThreshold * s) ) {
                BLink<V>[] tmp = new BLink[dirtyRange];

                for (int k = 0; k < dirtyRange; k++) {
                    tmp[k] = itemList.remove( dirtyStart /* removal position remains at the same index as items get removed */);
                }

                //TODO items.get(i) and
                //   ((FasterList) items.list).removeRange(dirtyStart+1, dirtyEnd);

                Collections.addAll(items, tmp);

                return this;
            }

            //((FasterList) itemList).sortThis(this);


            qsort( qsortStack, ((FasterList) itemList).array(), dirtyStart-1, itemList.size() );
        }

        return this;
    }

    final private int[] qsortStack = new int[32];



    public static int cmp(Object x, Object y) {
        return cmp((BLink)x, (BLink)y);
    }

    @SuppressWarnings({"unchecked"})
    public static void qsort(int[] stack, Object[] c, int start, int size) {
        int left = start, right = size - 1, stack_pointer = -1;
        while (true) {
            int i;
            int j;
            Object swap;
            if (right - left <= 7) {
                for (j = left + 1; j <= right; j++) {
                    swap = c[j];
                    i = j - 1;
                    while (i >= left && cmp(c[i], swap) > 0) {
                        c[i + 1] = c[i--];
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer == -1) {
                    break;
                }
                right = stack[stack_pointer--];
                left = stack[stack_pointer--];
            } else {
                int median = (left + right) >> 1;
                i = left + 1;
                j = right;
                swap = c[median];
                c[median] = c[i];
                c[i] = swap;
                if (cmp(c[left], c[right]) > 0) {
                    swap = c[left];
                    c[left] = c[right];
                    c[right] = swap;
                }
                if (cmp(c[i], c[right]) > 0) {
                    swap = c[i];
                    c[i] = c[right];
                    c[right] = swap;
                }
                if (cmp(c[left], c[i]) > 0) {
                    swap = c[left];
                    c[left] = c[i];
                    c[i] = swap;
                }
                Object temp = c[i];
                while (true) {
                    //noinspection ControlFlowStatementWithoutBraces,StatementWithEmptyBody
                    while (cmp(c[++i], temp) < 0) ;
                    //noinspection ControlFlowStatementWithoutBraces,StatementWithEmptyBody
                    while (cmp(c[--j], temp) > 0) ;
                    if (j < i) {
                        break;
                    }
                    swap = c[i];
                    c[i] = c[j];
                    c[j] = swap;
                }
                c[left + 1] = c[j];
                c[j] = temp;
                if (right - i + 1 >= j - left) {
                    stack[++stack_pointer] = i;
                    stack[++stack_pointer] = right;
                    right = j - 1;
                } else {
                    stack[++stack_pointer] = left;
                    stack[++stack_pointer] = j - 1;
                    left = i;
                }
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
    public float priMax() {
        return isEmpty() ? 0 : items.get(0).pri();
    }

    @Override
    public float priMin() {
        return isEmpty() ? 0 : items.get(items.size()-1).pri();
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
