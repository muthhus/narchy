package jcog.table;

import jcog.data.sorted.SortedArray;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Created by me on 1/15/16.
 */
abstract public class SortedListTable<V, L> extends ArrayListTable<V,L> implements SortedTable<V,L>, FloatFunction<L> {

    /**
     * array of lists of items, for items on different level
     */
    //protected final SortedList_1x4<L> items;
    protected final @NotNull SortedArray<L> items;


    public SortedListTable(SortedArray<L> items, @NotNull Map<V, L> map) {
        super(map);
        //this.items = new SortedList_1x4<>(items, this, searchType, false);
        this.items = items;
    }

    @NotNull
    @Override
    protected final Object _items() {
        return items;
    }

    @NotNull
    @Override
    public Iterator<L> iterator() {
        return items.iterator();
    }

    @Override
    public final L get(int i) {
        return items.array()[i];
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    protected final boolean listRemove(L removed) {
        return items.remove(removed, this);
    }

    @Override
    protected final void listAdd(L i) {
        items.add(i, this);
    }

    @Override
    protected final void listClear() {
        items.clear();
    }


    @Override @Nullable
    public final L top() {
        return (size()==0) ? null : get(0);
    }

    @Override @Nullable
    public final L bottom() {
        int s = size();
        return s == 0 ? null : get(s - 1);
    }



    /** gets the key associated with a value */
    @Nullable @Override
    abstract public V key(@NotNull L l);




    @Nullable
    @Override
    @Deprecated protected L addItem(@NotNull L i) {
        int cap = capacity();
        if (cap < 1) {
            return i; //bounce
        }

        int size = size();

        L displaced = null;

        if (size == cap) {
            L last = items.last();
            if (Float.compare(floatValueOf(last), floatValueOf(i)) < 0) {
                //insufficient rank, bounce
                return i;
            }

            displaced = items.removeLast(); //remove last
        }

        items.add(i, this);

        return displaced;
    }

    @NotNull
    @Deprecated public List<L> listCopy() {
        List<L> l = new ArrayList(size());
        forEach((Consumer<L>) l::add);
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
