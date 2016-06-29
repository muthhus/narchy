package nars.concept.table;

import nars.bag.Table;
import nars.util.CollectorMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Items are available by an integer index
 */
abstract public class ArrayListTable<V, L> extends CollectorMap<V, L> implements Table<V, L>, Iterable<L> {


    private int capacity = -1;

    public ArrayListTable(Map<V, L> map) {
        super(map);
    }

    abstract public L get(int i);

    @Override
    abstract public int size();

    @Override
    public final void forEachKey(@NotNull Consumer<? super V> each) {
        forEach(t -> each.accept(key(t)));
    }

    @Override
    abstract public Iterator<L> iterator();

    @Override
    public void topWhile(@NotNull Predicate<? super L> action, int n) {
        int s = size();
        if (n < 0)
            n = s;
        else
            n = Math.min(s, n);

        for (int i = 0; i < n; i++) {
            L x = get(i);
            if (x == null || (!action.test(x)))
                break;
        }
    }


    @Override
    public final void clear() {
        super.clear();
        listClear();
    }

    abstract protected void listClear();

    /**
     * Check if an item is in the bag
     *
     * @param k An item
     * @return Whether the Item is in the Bag
     */
    public final boolean contains(V k) {
        return this.containsKey(k);
    }


    @Nullable
    @Override
    protected L removeItem(@NotNull L removed) {
        return listRemove(removed) ? removed : null;
    }

    protected abstract boolean listRemove(L removed);


    @Nullable
    @Override
    protected L addItem(L i) {
        if (isFull())
            throw new RuntimeException("table full");

        listAdd(i);
        return null;
    }

    protected abstract void listAdd(L i);


    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @return The first Item
     */
    @NotNull
    public final L removeItem(int index) {

        L ii = get(index);
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
    final public void setCapacity(int newCapacity) {
        if (newCapacity != this.capacity) {
            this.capacity = newCapacity;
            //int excess = size() - newCapacity;
            //while (excess-- > 0)

            while (size() - newCapacity > 0)
                removeWeakest("Shrink");

        }
    }

    protected abstract void removeWeakest(Object reason);

    abstract public V weakest();

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
    public void forEach(int max, @NotNull Consumer<? super L> action) {
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
