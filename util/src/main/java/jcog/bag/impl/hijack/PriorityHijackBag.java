package jcog.bag.impl.hijack;

import jcog.bag.impl.HijackBag;
import jcog.pri.PriMerge;
import jcog.pri.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 2/17/17.
 */
abstract public class PriorityHijackBag<K,V extends Priority> extends HijackBag<K, V> {


    public PriorityHijackBag(int reprobes) {
        super(reprobes);
    }

    @Override
    protected V merge(@Nullable V existing, @NotNull V incoming, float scale) {
        float pressure = PriMerge.combine(existing, incoming, scale);
        if (pressure >= Priority.EPSILON_DEFAULT)
            pressurize(pressure);
        return existing!=null ? existing : incoming; //default to the original instance
    }

    @Override
    public float pri(@NotNull V key) {
        return key.pri();
    }

}