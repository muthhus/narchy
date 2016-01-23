package nars.bag.impl;

import nars.bag.Table;
import nars.util.CollectorMap;
import nars.util.data.sorted.SortedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 1/15/16.
 */
abstract public class ArrayTable<V, L> extends CollectorMap<V,L> implements Table<V,L> {
    /**
     * array of lists of items, for items on different level
     */
    public final SortedIndex<L> items;

    public ArrayTable(SortedIndex<L> items, Map<V, L> map) {
        super(map);
        this.items = items;
    }


    @Override
    public final void forEachKey(@NotNull Consumer<? super V> each) {
        forEach(t -> each.accept(key(t)));
    }


//    @Override
//    public L put(V v, L l) {
//        return this.put(v, l);
//    }

    public boolean isSorted() {
        return items.isSorted();
    }

    @Override
    public final void clear() {
        super.clear();
        items.clear();
    }

//    /**
//     * The number of items in the bag
//     *
//     * @return The number of items
//     */
//    @Override
//    public final int size() {
//        /*if (Global.DEBUG)
//            validate();*/
//        return items.size();
//    }

    public final void setCapacity(int capacity) {
        items.setCapacity(capacity);
    }

    /**
     * Check if an item is in the bag
     *
     * @param k An item
     * @return Whether the Item is in the Bag
     */
    public boolean contains(V k) {
        return this.containsKey(k);
    }



    @Override
    public int size() {
        return items.size(); //eternal.size() + temporal.size();
    }

    /**
     * TODO make this work for the original condition: (size() >= capacity)
     * all comparisons like this should use this same condition
     */
    final boolean isFull() {
        return (size() >= capacity());
    }

    protected final L removeBottom() {
        if (isEmpty()) return null;
        return removeItem(size() - 1);
    }

    public final L top() {
        if (isEmpty()) return null;
        return getItem(0);
    }

    public final L bottom() {
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
    @Nullable
    final L removeItem(int index) {

        L ii = getItem(index);
        /*if (ii == null)
            throw new RuntimeException("invalid index: " + index + ", size=" + size());*/

        //        if (ii!=jj) {
//            throw new RuntimeException("removal fault");
//        }

        removeItem(ii);
        removeKeyForValue(ii);
        return ii;
    }

    /** gets the key associated with a value */
    abstract public V key(L l);

//    @Override
//    public L get(Object key) {
//        return index.get(key);
//    }

    public final int capacity() {
        return items.capacity();
    }

    @Override
    public final void forEach(@NotNull Consumer<? super L> action) {

        items.getList().forEach(action);

//        //items.forEach(b -> action.accept(b.get()));
//
//        final List<? extends L> l = items.getList();
//
//        int n = l.size();
//        for (int i = 0; i < n; i++) {
//        //for (int i = l.size()-1; i >= 0; i--){
//            action.accept(l.get(i));
//        }

    }

    @NotNull
    @Override
    public final Iterator<L> iterator() {
        return items.iterator();
    }

    /**
     * default implementation; more optimal implementations will avoid instancing an iterator
     */
    public void forEach(int max, @NotNull Consumer<? super L> action) {
        List<? extends L> l = items.getList();
        int n = Math.min(l.size(), max);
        //TODO let the list implementation decide this because it can use the array directly in ArraySortedIndex
        for (int i = 0; i < n; i++) {
            action.accept(l.get(i));
        }
    }

    @Override
    public void topWhile(@NotNull Predicate<L> action) {
        List<? extends L> l = items.getList();
        int n = l.size();
        for (int i = 0; i < n; i++) {
            if (!action.test(l.get(i)))
                break;
        }
    }



//    @Override
//    public final void topN(int limit, @NotNull Consumer action) {
//        List l = items.getList();
//        int n = Math.min(l.size(), limit);
//        for (int i = 0; i < n; i++)
//            action.accept(l.get(i));
//    }


    @Override
    protected final L removeItem(L removed) {

        return items.remove(removed) ? removed : null;

    }

    @Override
    protected final L addItem(L i) {
        L overflow = items.insert(i);
        if (overflow!=null) {
            L v = removeKeyForValue(overflow);
            if (v!=overflow)
                throw new RuntimeException("bag inconsistency: " + overflow + " mismatched with " + v);
        }
        return overflow;
    }

//    protected final class ArrayMapping extends CollectorMap<V, L> {
//
//        final SortedIndex<L> items;
//
//        public ArrayMapping(Map<V, L> map, SortedIndex<L> items) {
//            super(map);
//            this.items = items;
//        }
//
//
//    }
}
