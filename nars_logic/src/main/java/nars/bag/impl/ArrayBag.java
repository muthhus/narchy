package nars.bag.impl;

import com.google.common.collect.Sets;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.*;
import nars.util.ArraySortedIndex;
import nars.util.data.sorted.SortedIndex;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 */
public class ArrayBag<V> extends ArrayTable<V,BLink<V>> implements Bag<V> {

    //public static final Procedure2<Budget, Budget> DEFAULT_MERGE_METHOD = UnitBudget.average;

    protected BudgetMerge mergeFunction = null;

    public ArrayBag(int cap) {
        this(new BudgetedArraySortedIndex<>(cap));
    }

    public ArrayBag(SortedIndex<BLink<V>> items) {
        this(items,
            //Global.newHashMap(items.capacity()/2)
            new HashMap(items.capacity()/2)
        );
    }

    public ArrayBag(SortedIndex<BLink<V>> items, Map<V, BLink<V>> map) {
        super(items, map);

        items.clear();
        setCapacity(items.capacity());
    }


    Bag<V> setMergeFunction(BudgetMerge mergeFunction) {
        this.mergeFunction = mergeFunction;
        return this;
    }

    @Override
    public final V key(BLink<V> l) {
        return l.get();
    }

    @Override public BLink<V> put(Object v) {
        //TODO combine with CurveBag.put(v)
        BLink<V> existing = get(v);
        if (existing!=null) {
            merge(existing.getBudget(),
                    getDefaultBudget(v), 1f);
            return existing;
        } else {
            return existing != null ? existing :
                    put((V) v, getDefaultBudget(v));
        }
    }


    /**
     * set the merging function to 'plus'
     */
    public Bag<V> mergePlus() {
        return setMergeFunction(BudgetMerge.plusDQDominated);
    }

//    /**
//     * sets a null merge function, which can be used to detect
//     * merging which should not happen (it will throw null pointer exception)
//     */
//    Bag<V> mergeNull() {
//        return setMergeFunction(null);
//    }

    protected final void merge(Budget target, Budget incoming, float scale) {
        mergeFunction.merge(target, incoming, scale);
    }

    private Budget getDefaultBudget(Object v) {
        if (v instanceof Budgeted)
            return ((Budgeted)v).getBudget();
        return UnitBudget.zero;
    }


    public BLink<V> put(BudgetedHandle k) {
        return put(k, k.getBudget());
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
    public BLink<V> sample() {
        throw new RuntimeException("unimpl");
    }

    @Override
    public Bag<V> sample(int n, Predicate<BLink<V>> each, Collection<BLink<V>> target) {
        throw new RuntimeException("unimpl");
    }

    public void validate() {
        int in = index.size();
        int is = items.size();
        if (Math.abs(is - in) > 0) {
//                System.err.println("INDEX");
//                for (Object o : index.values()) {
//                    System.err.println(o);
//                }
//                System.err.println("ITEMS:");
//                for (Object o : items) {
//                    System.err.println(o);
//                }

            Set<V> difference = Sets.symmetricDifference(
                    new HashSet(index.values()),
                    new HashSet(items)
            );

            System.err.println("DIFFERENCE");
            for (Object o : difference) {
                System.err.println("  " + o);
            }

            throw new RuntimeException("curvebag fault: " + in + " index, " + is + " items");
        }

//            //test for a discrepency of +1/-1 difference between name and items
//            if ((is - in > 2) || (is - in < -2)) {
//                System.err.println(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
//                /*System.out.println(nameTable);
//                System.out.println(items);
//                if (is > in) {
//                    List<E> e = new ArrayList(items);
//                    for (E f : nameTable.values())
//                        e.remove(f);
//                    System.out.println("difference: " + e);
//                }*/
//                throw new RuntimeException(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
//            }
    }

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
    public final BLink<V> put(Object i, Budget b, float scale) {

        ArrayMapping index = this.index;

        BLink<V> existing = index.get(i);

        if (existing != null) {

            if (existing!=b)
                merge(existing, b, scale);

            update(existing);

            return existing;

        } else {

            BLink newBudget;
            if (!(b instanceof BLink)) {
                newBudget = new BLink(i, b, scale);
            } else {
                //use provided
                newBudget = (BLink)b;
                newBudget.commit();
            }

            BLink<V> displaced = index.put((V) i, newBudget);
            if (displaced!=null) {
                if (displaced == newBudget)
                    return null; //wasnt inserted
                else {
                    //remove what was been removed from the items list
                    index.removeKey(displaced.get());
                }
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
        top(this::update);
    }

    public void update(BLink<V> v) {
        if (!v.hasDelta()) {
            return;
        }
        int size = size();
        if (size == 1) {
            v.commit();
            return;
        }

        SortedIndex ii = this.items;

        int currentIndex = ii.locate(v);
        if (currentIndex == -1) {
            //an update for an item which has been removed already. must be re-inserted
            v.commit();
            put(v);
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
        if (isEmpty()) return null;
        return removeItem(0);
    }

    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }


    @Override
    public final Iterator<BLink<V>> iterator() {
        return items.iterator();
    }

    @Override
    public float getPriorityMax() {
        if (isEmpty()) return 0;
        return items.getFirst().getPriority();
    }

    @Override
    public float getPriorityMin() {
        if (isEmpty()) return 0;
        return items.getLast().getPriority();
    }

    public final void popAll(Consumer<BLink<V>> receiver) {
        forEach(receiver);
        clear();
    }

    public void pop(Consumer<BLink<V>> receiver, int n) {
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

        @Override public float score(X v) {
            return v.getPriority();
        }
    }
}
