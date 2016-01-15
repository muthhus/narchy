package nars.bag.impl;

import nars.bag.Link;
import nars.util.CollectorMap;
import nars.util.data.sorted.SortedIndex;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 1/15/16.
 */
public abstract class ArrayTable<V, L extends Link<V>> implements Table<V,L> {
    /**
     * mapping from key to item
     */
    public final ArrayMapping index;
    /**
     * array of lists of items, for items on different level
     */
    public final SortedIndex<? extends L> items;

    public ArrayTable(SortedIndex<L> items, Map<V, L> map) {
        this.items = items;
        this.index = new ArrayMapping(map, items);
    }

    public abstract L put(Object v);

    @Override
    public L put(V v, L l) {
        throw new RuntimeException("unimpl");
    }

    public boolean isSorted() {
        return items.isSorted();
    }

    public final void clear() {
        items.clear();
        index.clear();
    }

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    public final int size() {
        /*if (Global.DEBUG)
            validate();*/
        return items.size();
    }

    public final void setCapacity(int capacity) {
        items.setCapacity(capacity);
    }

    /**
     * Check if an item is in the bag
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    public boolean contains(V it) {
        return index.containsKey(it);
    }

    public L remove(V key) {
        return index.remove(key);
    }

    /**
     * TODO make this work for the original condition: (size() >= capacity)
     * all comparisons like this should use this same condition
     */
    final boolean isFull() {
        return (size() >= capacity());
    }

    protected final L removeLowest() {
        if (isEmpty()) return null;
        return removeItem(size() - 1);
    }

    public final L highest() {
        if (isEmpty()) return null;
        return getItem(0);
    }

    public final L lowest() {
        if (isEmpty()) return null;
        return getItem(size() - 1);
    }

    final L getItem(int index) {
        return items.get(index);
    }

    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @return The first Item
     */
    final L removeItem(int index) {

        L ii = getItem(index);
        /*if (ii == null)
            throw new RuntimeException("invalid index: " + index + ", size=" + size());*/

        //        if (ii!=jj) {
//            throw new RuntimeException("removal fault");
//        }

        return remove(ii.get());
    }

    public L get(Object key) {
        return index.get(key);
    }

    public final int capacity() {
        return items.capacity();
    }

    public final void forEach(Consumer<? super V> action) {

        //items.forEach(b -> action.accept(b.get()));

        final List<? extends L> l = items.getList();

        //start at end
        int n = l.size();
        for (int i = 0; i < n; i++) {
        //for (int i = l.size()-1; i >= 0; i--){
            action.accept(l.get(i).get());
        }

    }

    /**
     * default implementation; more optimal implementations will avoid instancing an iterator
     */
    public void forEach(int max, Consumer<? super V> action) {
        List<? extends L> l = items.getList();
        int n = Math.min(l.size(), max);
        //TODO let the list implementation decide this because it can use the array directly in ArraySortedIndex
        for (int i = 0; i < n; i++) {
            action.accept(l.get(i).get());
        }
    }

    public void topWhile(Predicate<L> action) {
        List<? extends L> l = items.getList();
        int n = l.size();
        for (int i = 0; i < n; i++) {
            if (!action.test(l.get(i)))
                break;
        }
    }

    public final void top(Consumer<L> action) {
        items.getList().forEach(action);
    }

    public void topN(int limit, Consumer action) {
        List l = items.getList();
        int n = Math.min(l.size(), limit);
        for (int i = 0; i < n; i++)
            action.accept(l.get(i));
    }

    public abstract float getPriorityMax();

    public abstract float getPriorityMin();

    protected final class ArrayMapping extends CollectorMap<V, L> {

        final SortedIndex<L> items;

        public ArrayMapping(Map<V, L> map, SortedIndex<L> items) {
            super(map);
            this.items = items;
        }


        @Override
        protected L removeItem(L removed) {

            if (items.remove(removed)) {
                return removed;
            }

            return null;
        }

        @Override
        protected L addItem(L i) {
            L overflow = items.insert(i);
            if (overflow!=null) {
                removeKey(overflow.get());
            }
            return overflow;
        }
    }
}
