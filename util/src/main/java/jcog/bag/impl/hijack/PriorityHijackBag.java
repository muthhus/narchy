package jcog.bag.impl.hijack;

import jcog.bag.impl.HijackBag;
import jcog.pri.Priority;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/17/17.
 */
abstract public class PriorityHijackBag<K,V extends Priority> extends HijackBag<K, V> {


    public PriorityHijackBag(int reprobes) {
        super(reprobes);
    }
    public PriorityHijackBag(int cap, int reprobes) {
        this(reprobes);
        setCapacity(cap);
    }


    @Override
    protected V merge(@NotNull V existing, @NotNull V incoming) {
        existing.priAdd(incoming.priSafe(0));
        return existing; //default to the original instance
    }

    @Override
    public float pri(@NotNull V key) {
        return key.pri();
    }

}
