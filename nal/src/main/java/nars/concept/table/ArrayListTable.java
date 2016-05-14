package nars.concept.table;

import nars.bag.impl.ListTable;
import nars.util.CollectorMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 5/7/16.
 */
abstract public class ArrayListTable<V,L> extends CollectorMap<V,L> implements ListTable<V,L>, Iterable<L> {


    private final List<L> list;
    private int capacity;

    public ArrayListTable(Map<V,L> map, List<L> l) {
        super(map);
        this.list = l;
    }

    @Override
    public final int size() {
        return list.size();
    }

    @Override
    public final void forEachKey(@NotNull Consumer<? super V> each) {
        forEach(t -> each.accept(key(t)));
    }

    @NotNull
    @Override public Iterator<L> iterator() {
        return list.iterator();
    }

    @Override
    public void topWhile(@NotNull Predicate<L> action) {
        List<L> l = list();
        int n = l.size();
        for (int i = 0; i < n; i++) {
            if (!action.test(l.get(i)))
                break;
        }
    }

    @Override
    public final void clear() {
        super.clear();
        list().clear();
    }

    /**
     * Check if an item is in the bag
     *
     * @param k An item
     * @return Whether the Item is in the Bag
     */
    public final boolean contains(V k) {
        return this.containsKey(k);
    }


    @Override
    public final List<L> list() {
        return list;
    }

    @Nullable
    @Override
    protected L removeItem(L removed) {
        return list.remove(removed) ? removed : null;
    }


    @Nullable
    @Override
    protected L addItem(L i) {
        if (isFull())
            throw new RuntimeException("table full");

        list.add(i);
        return null;
    }

    @NotNull
    public final L item(int index) {
        return list().get(index);
    }

    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @return The first Item
     */
    @NotNull
    public final L removeItem(int index) {

        L ii = item(index);
        /*if (ii == null)
            throw new RuntimeException("invalid index: " + index + ", size=" + size());*/

        //        if (ii!=jj) {
//            throw new RuntimeException("removal fault");
//        }

        removeItem(ii);
        removeKeyForValue(ii);
        return ii;
    }


    @Override
    public final int capacity() {
        return capacity;
    }

    @Override
    public void setCapacity(int newCapacity) {
        int currentCap = this.capacity;
        if (newCapacity!=currentCap) {
            this.capacity = newCapacity;
            int excess = size() - newCapacity;
            while (excess-- > 0)
                removeWeakest("Shrink");
        }
    }

    protected abstract void removeWeakest(Object reason);

    abstract public V weakest();

    @Override
    public final void forEach(@NotNull Consumer<? super L> action) {

        list().forEach(action);

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

    /**
     * default implementation; more optimal implementations will avoid instancing an iterator
     */
    public void forEach(int max, @NotNull Consumer<? super L> action) {
        List<? extends L> l = list();
        int n = Math.min(l.size(), max);
        //TODO let the list implementation decide this because it can use the array directly in ArraySortedIndex
        for (int i = 0; i < n; i++) {
            action.accept(l.get(i));
        }
    }

//    @Nullable
//    @Override
//    public Object remove(Object key) {
//        if (list.remove(key)) {
//            return key;
//        }
//        return null;
//    }

}
