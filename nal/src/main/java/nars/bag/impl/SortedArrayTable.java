package nars.bag.impl;

import nars.concept.table.ArrayListTable;
import org.happy.collections.lists.decorators.SortedList_1x4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * Created by me on 1/15/16.
 */
abstract public class SortedArrayTable<V, L> extends ArrayListTable<V,L> implements SortedTable<V,L>, Comparator<L> {

    /**
     * array of lists of items, for items on different level
     */
    @NotNull
    protected final SortedList_1x4<L> items;


    public SortedArrayTable(List<L> items, Map<V, L> map) {
        this(items, map, SortedList_1x4.SearchType.BinarySearch);
    }

    public SortedArrayTable(List<L> items, Map<V, L> map, SortedList_1x4.SearchType searchType) {
        super(map, items);
        this.items = new SortedList_1x4<>(items, this, searchType, false);
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


    @Nullable
    @Override
    protected L addItem(L i) {

        int size = size();

        L displaced = null;
        if (size == capacity()) {
            if (compare(items.last(), i) < 0) {
                //insufficient rank, bounce
                return i;
            }

            displaced = items.remove(size - 1); //remove last
        }

        items.add(i);

        return displaced;
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
