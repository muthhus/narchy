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


    public SortedListTable(IntFunction<L[]> builder, Map<V, L> map) {
        super(map);
        //this.items = new SortedList_1x4<>(items, this, searchType, false);
        this.items = new SortedArray<>(builder);
    }


    @NotNull
    @Override
    public final Iterator<L> iterator() {
        return items.iterator();
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
        return items.remove(removed, this);
    }

    @Override
    protected void listAdd(L i) {
        items.add(i, this);
    }

    @Override
    protected void listClear() {
        items.clear();
    }


    @Override @Nullable
    public L top() {
        return (size()==0) ? null : get(0);
    }

    @Override @Nullable
    public L bottom() {
        int s = size();
        return s == 0 ? null : get(s - 1);
    }



    /** gets the key associated with a value */
    @Nullable @Override
    abstract public V key(L l);


    @Override
    @NotNull public V weakest() {
        @Nullable L w = bottom();
        return w == null ? null : key(w);
    }

    @Nullable
    @Override
    protected L addItem(L i) {
        int cap = capacity();
        if (cap < 1) {
            //bounce
            return i;
        }

        int size = size();

        L displaced = null;

        if (size == cap) {
            L last = items.last();
            if (compare(last, i) < 0) {
                //insufficient rank, bounce
                return i;
            }

            displaced = items.removeWeakest(); //remove last
        }

        items.add(i, this);

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
