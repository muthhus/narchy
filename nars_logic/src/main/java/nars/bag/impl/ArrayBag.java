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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
    protected BudgetMerge mergeFunction = null;

    public ArrayBag(int cap) {
        this(new BudgetedArraySortedIndex<>(cap));
    }

    public ArrayBag(@NotNull SortedIndex<BLink<V>> items) {
        this(items,
            Global.newHashMap(items.capacity()/2)
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

    @Nullable
    @Override public BLink<V> put(V v) {
        //TODO combine with CurveBag.put(v)
        BLink<V> existing = get(v);
        if (existing!=null) {
            merge(existing.budget(),
                    getDefaultBudget(v), 1f);
            return existing;
        } else {
            return existing != null ? existing :
                    put((V) v, getDefaultBudget(v));
        }
    }

    protected final void merge(Budget target, Budget incoming, float scale) {
        mergeFunction.merge(target, incoming, scale);
    }

    private Budget getDefaultBudget(V v) {
        if (v instanceof Budgeted)
            return ((Budgeted)v).budget();
        return UnitBudget.zero;
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
    public Bag<V> sample(int n, Collection<? super BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }

    @Override
    public Bag<V> filter(Predicate<BLink<? extends V>> forEachIfFalseThenRemove) {
        List<BLink<V>> l = items.list();
        int n = l.size();
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                BLink<V> h = l.get(i);
                if (!forEachIfFalseThenRemove.test(h)) {
                    removeKeyForValue(h); //only remove key, we remove the item here
                    h.delete();
                    l.remove(i);
                    i--;
                    n--;
                }
            }
            commit();
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
        throw new RuntimeException("unimpl");
    }



    /**
     * Insert an item into the itemTable
     *
     * @param i The Item to put in
     * @return The updated budget
     */
    @Override
    public final BLink<V> put(V i, Budget b, float scale) {

        BLink<V> existing = get(i);

        if (existing != null) {

            if (existing!=b)
                merge(existing, b, scale);

            update(existing);

            return existing;

        } else {

            BLink<V> newBudget;
            if (!(b instanceof BLink)) {
                newBudget = new BLink<V>(i, b, scale);
            } else {
                //use provided
                newBudget = (BLink)b;
                newBudget.commit();
            }

            BLink<V> displaced = put( i, newBudget);
            if (displaced!=null) {
                if (displaced == newBudget) {
                    return null; //wasnt inserted
                }
                /*else {
                    //remove what was been removed from the items list
                    removeKey(displaced.get());
                }*/
            }

            return newBudget;

        }

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



    @Override
    public final void commit() {
        forEach(this::update);
    }

    public void update(@NotNull BLink<V> v) {
        if (!v.hasDelta()) {
            return;
        }
        int size = size();
        if (size == 1) {
            v.commit();
            /*if (!v.commit()) {

            }*/
            return;
        }

        SortedIndex ii = this.items;

        int currentIndex = ii.locate(v);
        if (currentIndex == -1) {
            //an update for an item which has been removed already. must be re-inserted
            v.commit();
            put(v.get(), v);
            return;
        }

        v.commit();

        float newScore = ii.score(v);

        if ((newScore < ii.scoreAt(currentIndex+1, size)) || //score of item below
                (newScore > ii.scoreAt(currentIndex-1, size)) //score of item above
            ) {
            ii.remove(currentIndex);
            ii.insert(v); //reinsert
        } else {
            //otherwise, it remains in the same position and move unnecessary
        }
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
    public float getPriorityMax() {
        return isEmpty() ? 0 : items.getFirst().pri();
    }

    @Override
    public float getPriorityMin() {
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


    public final static class BudgetedArraySortedIndex<X extends Budgeted> extends ArraySortedIndex<X> {
        public BudgetedArraySortedIndex(int capacity) {
            super(capacity / 4, capacity);
        }

        @Override public float score(@NotNull X v) {
            return v.pri();
        }
    }
}
