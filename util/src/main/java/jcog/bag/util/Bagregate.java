package jcog.bag.util;

import jcog.bag.impl.ArrayBag;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import jcog.data.FloatParam;
import jcog.pri.PriMerge;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * a bag which wraps another bag, accepts its value as input but at a throttled rate
 * resulting in containing effectively the integrated / moving average values of the input bag
 * TODO make a PLink version of ArrayBag since quality is not used here
 */
public class Bagregate<X> extends ArrayBag<X> {

    private final Iterable<PLink<X>> src;
    private final MutableFloat scale;
    final AtomicBoolean busy = new AtomicBoolean();

    public Bagregate(@NotNull Iterable<PLink<X>> src, int capacity, float scale) {
        super(capacity, PriMerge.avgBlend, new ConcurrentHashMap<>(capacity));

        this.src = src;
        this.scale = new FloatParam(scale);

        update();
    }

    protected void update() {
        if (!busy.compareAndSet(false, true))
            return;

        //try {

        commit();

        float scale = this.scale.floatValue();

        src.forEach(p -> {


            X x = p.get();
            float pri = p.pri();


            if (x != null && include(x)) {
                put(new RawPLink(x, pri), scale, null);
            }
        });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        busy.set(false);
    }

    /**
     * can be overridden to filter entry
     */
    protected boolean include(X x) {
        return true;
    }

    @Override
    public void forEach(Consumer<? super PLink<X>> action) {
        forEach(size(), action);
    }

    @Override
    public void forEach(int max, Consumer<? super PLink<X>> action) {
        update();
        super.forEach(max, action);
    }

}
