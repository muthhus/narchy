package nars.derive;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.Pri;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.IntFloatPredicate;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class ValueCache extends RecycledSummaryStatistics {

    private final FloatSupplier[] src;

    public final static float epsilon = 0.01f;
    /**
     * cache of value values
     */
    public final float[] value;
    public final AtomicLong now;

    public <X> ValueCache(Function<X, FloatSupplier> y, X...x) {
       this(Util.map(y, new FloatSupplier[x.length], x));
    }

    public ValueCache(FloatSupplier[] src) {
        this.src = src;
        this.now = new AtomicLong(Tense.ETERNAL);
        this.value = new float[src.length];
    }

    public void update() {
        int i = 0;
        clear();
        for (FloatSupplier c : src)
            accept(value[i++] = c.asFloat());
    }

    public void update(long now) {
        if (this.now.getAndSet(now) != now) {
            update();
        }
    }

    public void get(RoaringBitmap b, IntFloatPredicate each) {
        PeekableIntIterator ii = b.getIntIterator();
        int k = 0;
        while (ii.hasNext()) {
            int n = ii.next();
            if (!each.accept(n, value[n]))
                break;
        }
    }
    public void getNormalized(RoaringBitmap b, int levels, IntFloatPredicate each) {
        float min = (float)getMin();
        float max = (float)getMax();
        float range = max - min;
        if (Util.equals(Math.abs(max-min), 0, Pri.EPSILON)) {
            get(b, (c, v) -> each.accept(c, 0)); //flat
        } else {
            get(b, (c, v) -> each.accept(c,
                Util.bin((v - min) / range, levels)
            ));
        }
    }
}