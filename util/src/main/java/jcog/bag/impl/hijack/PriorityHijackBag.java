package jcog.bag.impl.hijack;

import jcog.bag.impl.HijackBag;
import jcog.pri.Pri;
import jcog.pri.PriMerge;
import jcog.pri.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by me on 2/17/17.
 */
abstract public class PriorityHijackBag<K,V extends Priority> extends HijackBag<K, V> {

    protected final PriMerge merge;

    public PriorityHijackBag(PriMerge merge, int reprobes) {
        super(reprobes);
        this.merge = merge;
    }

    @Override
    protected float merge(@Nullable V existing, @NotNull V incoming, float scale) {
        float inPri = incoming.priSafe(0);
        float pressure = inPri * scale;
        Priority applied;
        if (existing == null) {
            existing = incoming;
            applied = new Pri(0 );
            scale = 1f - scale; //?? does this actually work
        } else {
            applied = incoming;
        }

        //float pBefore = priSafe(existing, 0);
        merge.apply(existing, applied, scale); //TODO overflow
        //return priSafe(existing, 0) - pBefore;

        return pressure;
    }

    @Override
    protected Consumer<V> forget(float rate) {
        return null;
    }


    @Override
    public float pri(@NotNull V key) {
        return key.pri();
    }

}
