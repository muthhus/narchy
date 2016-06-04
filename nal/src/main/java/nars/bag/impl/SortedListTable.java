package nars.bag.impl;

import nars.concept.table.ArrayListTable;
import nars.util.data.sorted.SortedArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;


/**
 * Created by me on 1/15/16.
 */
abstract public class SortedListTable<V, L> extends ArrayListTable<V,L> implements SortedTable<V,L>, Comparator<L> {

    /**
     * array of lists of items, for items on different level
     */
    //protected final SortedList_1x4<L> items;
    protected final @NotNull SortedArray<L> items;


    public SortedListTable(IntFunction<L[]> builder, Map<V, L> map, SortedArray.SearchType searchType) {
        super(map);
        //this.items = new SortedList_1x4<>(items, this, searchType, false);
        this.items = new SortedArray<>(builder, this, searchType, 1);
    }

    @NotNull
    @Override
    public Iterator<L> iterator() {
        //throw new UnsupportedOperationException();
        return new ArrayIterator(items.array(), 0, items.size());
    }

    static class ArrayIterator<E> implements ListIterator<E> {
        private final E[] array;
        private final int size;
        private int next;
        private int lastReturned;

        protected ArrayIterator( E[] array, int index, int size ) {
            this.array = array;
            next = index;
            lastReturned = -1;
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return next != size;
        }

        @Override
        public E next() {
            if( !hasNext() )
                throw new NoSuchElementException();
            lastReturned = next++;
            return array[lastReturned];
        }

        @Override
        public boolean hasPrevious() {
            return next != 0;
        }

        @Override
        public E previous() {
            if( !hasPrevious() )
                throw new NoSuchElementException();
            lastReturned = --next;
            return array[lastReturned];
        }

        @Override
        public int nextIndex() {
            return next;
        }

        @Override
        public int previousIndex() {
            return next - 1;
        }

        @Override
        public void remove() {
            // This operation is not so easy to do but we will fake it.
            // The issue is that the backing list could be completely
            // different than the one this iterator is a snapshot of.
            // We'll just remove(element) which in most cases will be
            // correct.  If the list had earlier .equals() equivalent
            // elements then we'll remove one of those instead.  Either
            // way, none of those changes are reflected in this iterator.
            //DirectCopyOnWriteArrayList.this.remove(array[lastReturned]);
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public L get(int i) {
        return items.array()[i];
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    protected boolean listRemove(L removed) {
        return items.remove(removed);
    }

    @Override
    protected void listAdd(L i) {
        items.add(i);
    }

    @Override
    protected void listClear() {
        items.clear();
    }

    @Override @Nullable
    public L top() {
        return isEmpty() ? null : item(0);
    }

    @Override @Nullable
    public L bottom() {
        int s = size();
        return s == 0 ? null : item(s - 1);
    }



    /** gets the key associated with a value */
    @Nullable @Override
    abstract public V key(L l);


    @Override
    @NotNull public V weakest() {
        return key(bottom());
    }

    @Nullable
    @Override
    protected L addItem(L i) {

        int size = size();

        L displaced = null;
        if (size > 0 && size == capacity()) {
            if (compare(items.last(), i) < 0) {
                //insufficient rank, bounce
                return i;
            }

            displaced = items.removeLast(); //remove last
        }

        items.add(i);

        return displaced;
    }

    @NotNull
    @Deprecated public List<L> listCopy() {
        List<L> l = new ArrayList(size());
        forEach(x -> l.add(x));
        return l;
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
