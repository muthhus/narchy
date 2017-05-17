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

    @Override
    protected V merge(@NotNull V existing, @NotNull V incoming, float scale) {
        existing.priAdd(incoming.priSafe(0) * scale);
        return existing; //default to the original instance
    }

    @Override
    public float pri(@NotNull V key) {
        return key.pri();
    }

}
