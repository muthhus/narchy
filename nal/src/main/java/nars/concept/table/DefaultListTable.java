package nars.concept.table;

import nars.util.data.list.FasterList;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;


public abstract class DefaultListTable<V,L> extends ArrayListTable<V,L> {

    public final FasterList<L> list;

    public DefaultListTable(Map<V,L> map) {
        super(map);
        this.list = new FasterList<>(0);
    }

    @Override
    protected boolean listRemove(L removed) {
        return list.remove(removed);
    }

    @Override
    public L get(int i) {
        return list.get(i);
    }


    @Override
    protected void listAdd(@NotNull L i) {
        if (i == null)
            throw new NullPointerException();
        list.add(i);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Iterator<L> iterator() {
        return list.iterator();
    }

    @Override
    protected void listClear() {
        list.clear();
    }
}
