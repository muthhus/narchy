package nars.table;

import nars.bag.Table;
import nars.util.map.CollectorMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Items are available by an integer index
 */
abstract public class ArrayListTable<K, V> extends CollectorMap<K, V> implements Table<K, V>, Iterable<V> {


    protected int capacity;

    public ArrayListTable(@NotNull Map<K, V> map) {
        super(map);
    }

    abstract public V get(int i);

    @Override
    abstract public int size();

    @Override
    public final void forEachKey(@NotNull Consumer<K> each) {
        forEach(t -> {
            each.accept(key(t));
        });
    }

    @NotNull
    @Override
    abstract public Iterator<V> iterator();

    @Override
    public void topWhile(@NotNull Predicate<? super V> action, int n) {
        int s = size();
        if (n < 0)
            n = s;
        else
            n = Math.min(s, n);

        for (int i = 0; i < n; i++) {
            V x = get(i);
            if (x == null || (!action.test(x)))
                break;
        }
    }


    @Override
    public void clear() {
        synchronized (_items()) {
            super.clear(); //clears map
            listClear();
        }
    }

    abstract protected void listClear();

    /**
     * Check if an item is in the bag
     *
     * @param k An item
     * @return Whether the Item is in the Bag
     */
    public final boolean contains(@NotNull K k) {
        return this.containsKey(k);
    }


    @Nullable
    @Override
    protected final V removeItem(@NotNull V removed) {
        return listRemove(removed) ? removed : null;
    }

    protected abstract boolean listRemove(V removed);


    @Nullable
    @Override
    protected V addItem(@NotNull V i) {
        if (isFull())
            throw new RuntimeException("table full");

        listAdd(i);
        return null;
    }

    protected abstract void listAdd(V i);


    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @return The first Item
     */
    @NotNull
    public final V removeItem(int index) {

        V ii = get(index);
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

    /**
     * returns whether the capacity has changed
     */
    @Override
    public final boolean setCapacity(int newCapacity) {
        if (newCapacity != this.capacity) {
            synchronized (_items()) {
                this.capacity = newCapacity;
                if (this.size() > newCapacity)
                    commit();
            }
            return true;
        }
        return false;
    }

    /** a commit should invoke update(null) when its finished
     * @return this instance, HACK due to inheritance fuckup
     * */
    @NotNull
    protected abstract Object commit();

    /** if v is non-null it will be added after making capacity for it */
    protected abstract boolean update(@Nullable V v);


//    @Override
//    public final void forEach(@NotNull Consumer<? super L> action) {
//
//        list().forEach(action);
//
////        //items.forEach(b -> action.accept(b.get()));
////
////        final List<? extends L> l = items.getList();
////
////        int n = l.size();
////        for (int i = 0; i < n; i++) {
////        //for (int i = l.size()-1; i >= 0; i--){
////            action.accept(l.get(i));
////        }
//
//    }

    /**
     * default implementation; more optimal implementations will avoid instancing an iterator
     */
    public void forEach(int max, @NotNull Consumer<? super V> action) {
        int n = Math.min(size(), max);
        //TODO let the list implementation decide this because it can use the array directly in ArraySortedIndex
        for (int i = 0; i < n; i++) {
            action.accept(get(i));
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
