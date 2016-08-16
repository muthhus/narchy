package nars.concept.table;

import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;


public abstract class DefaultListTable<V,L> extends ArrayListTable<V,L> {

    @NotNull
    public final FasterList<L> list;

    public DefaultListTable(@NotNull Map<V,L> map) {
        super(map);
        this.list = new FasterList<>(0);
    }

    @Override
    protected final Object _items() {
        return list;
    }

    @Override
    protected final boolean listRemove(L removed) {
        return list.remove(removed);
    }

    @Override
    public final L get(int i) {
        return list.get(i);
    }


    @Override
    protected final void listAdd(@NotNull L i) {
        list.add(i);
    }

    @Override
    public final int size() {
        return list.size();
    }

    @NotNull
    @Override
    public Iterator<L> iterator() {
        return list.iterator();
    }

    @Override
    protected void listClear() {
        list.clear();
    }

}
