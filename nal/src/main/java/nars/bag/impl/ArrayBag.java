package nars.bag.impl;

import nars.Global;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.budget.UnitBudget;
import nars.util.ArraySortedIndex;
import nars.util.data.sorted.SortedIndex;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 */
public class ArrayBag<V> extends ArrayTable<V,BLink<V>> implements Bag<V> {

    //public static final Procedure2<Budget, Budget> DEFAULT_MERGE_METHOD = UnitBudget.average;

    @Nullable
    protected BudgetMerge mergeFunction;

    public ArrayBag(int cap) {
        this(new BudgetedArraySortedIndex<>(cap));
    }

    public ArrayBag(@NotNull SortedIndex<BLink<V>> items) {
        this(items,
            Global.newHashMap(0) //start zero to minimize cost of creating temporary bags
            //new HashMap(items.capacity()/2)
        );
    }

    public ArrayBag(@NotNull SortedIndex<BLink<V>> items, Map<V, BLink<V>> map) {
        super(items, map);

        items.clear();
        setCapacity(items.capacity());
    }


    @NotNull
    public Bag<V> merge(BudgetMerge mergeFunction) {
        this.mergeFunction = mergeFunction;
        return this;
    }

    @Override
    public final V key(@NotNull BLink<V> l) {
        return l.get();
    }



    /** returns amount overflowed */
    protected final float merge(@NotNull BLink<V> target, Budgeted incoming, float scale) {
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
    public Bag<V> sample(int n, Consumer<? super BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }

    @NotNull
    @Override
    public Bag<V> filter(@NotNull Predicate<BLink<? extends V>> forEachIfFalseThenRemove) {
        List<BLink<V>> l = items.list();
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


    /**
     * Choose an Item according to priority distribution and take it out of the
     * Bag
     *
     * @return The selected Item, or null if this bag is empty
     */
    @Nullable
    @Override
    public BLink<V> pop() {
        throw new UnsupportedOperationException();
    }



    /**
     * Insert an item into the itemTable
     *
     * @param i The Item to put in
     * @return The updated budget
     */
    @Nullable
    @Override
    public BLink<V> put(V i, Budgeted b, float scale, @Nullable MutableFloat overflow) {
        //    }
        //
        //    public BLink<V> putFast(V i, Budgeted b, float scale) {
        //        BLink<V> existing = get(i);
        //
        //    }

            ///** updates an item, merging and re-inserting inserting */
            //public BLink<V> putSync(V i, Budgeted b, float scale) {
        BLink<V> existing = get(i);

        return (existing != null) ?
                putExists(b, scale, existing, overflow) :
                putNew(i, b, scale);

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

    @NotNull
    private BLink<V> putExists(Budgeted b, float scale, @NotNull BLink<V> existing, @Nullable MutableFloat overflow) {

        if (existing!=b) {

            float o = merge(existing, b, scale);
            if (overflow!=null)
                overflow.add(o);

            update(existing); //<- delaying/buffering this is the whole reason of the buffering
        }

        return existing;
    }

    @Nullable
    private BLink<V> putNew(V i, Budgeted b, float scale) {
        BLink<V> newBudget;
        if (!(b instanceof BLink)) {
            newBudget = new BLink<>(i, b, scale);
        } else {
            //use provided
            newBudget = (BLink)b;
            newBudget.commit();
        }


            BLink<V> displaced = put(i, newBudget);
            if (displaced != null) {
                if (displaced == newBudget) {
                    return null;
                }
                /*else {
                    //remove what was been removed from the items list
                    removeKey(displaced.get());
                }*/
            }

        return newBudget;
    }


    @Nullable
    @Override
    public Bag<V> commit() {
        forEach(this::update);
        return this;
    }

    public void update(@NotNull BLink<V> v) {
        if (!v.hasDelta()) {
            return;
        }

        SortedIndex ii = this.items;

        int size = ii.size();
        if (size == 1) {
            //its the only item
            v.commit();
            return;
        }


        int currentIndex = ii.locate(v);

        v.commit(); //after finding where it was, apply its updates to find where it will be next

        if (currentIndex == -1) {
            //an update for an item which has been removed already. must be re-inserted
            put(v.get(), v);
        } else if (ii.scoreBetween(currentIndex, size, v)) { //has position changed?
            ii.reinsert(currentIndex, v);
        }
        /*} else {
            //otherwise, it remains in the same position and a move is unnecessary
        }*/
    }

    protected final BLink<V> removeHighest() {
        return isEmpty() ? null : removeItem(0);
    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }


    @Override
    public float priMax() {
        return isEmpty() ? 0 : items.getFirst().pri();
    }

    @Override
    public float priMin() {
        return isEmpty() ? 0 : items.getLast().pri();
    }

    public final void popAll(@NotNull Consumer<BLink<V>> receiver) {
        forEach(receiver);
        clear();
    }

    public void pop(@NotNull Consumer<BLink<V>> receiver, int n) {
        if (n == size()) {
            //special case where size <= inputPerCycle, the entire bag can be flushed in one operation
            popAll(receiver);
        } else {
            for (int i = 0; i < n; i++) {
                receiver.accept(pop());
            }
        }
    }


    public float priAt(int cap) {
        if (size() <= cap) return 1f;
        return item(cap).pri();
    }


    public final static class BudgetedArraySortedIndex<X extends Budgeted> extends ArraySortedIndex<X> {
        public BudgetedArraySortedIndex(int capacity) {
            super(capacity / 4, capacity);
        }

        @Override public float score(@NotNull X v) {
            return v.pri();
        }
    }
}
