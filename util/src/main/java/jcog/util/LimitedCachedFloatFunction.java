package jcog.util;

import jcog.map.MRUCache;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

public class LimitedCachedFloatFunction<X> extends MRUCache<X, Float> implements FloatFunction<X> {

    private final FloatFunction<X> f;

    public LimitedCachedFloatFunction(FloatFunction<X> f, int capacity) {
        super(capacity);
        this.f = f;
    }

//    @Override
//    public final float floatValueOf(X x) {
//        return computeIfAbsent(x, f::floatValueOf);
//    }


    @Override
    public float floatValueOf(X x) {

        synchronized (f) {
            Float f = get(x);
            if (f != null)
                return f;
        }

        float v = f.floatValueOf(x);

        synchronized (f) {
            put(x, v);
        }

        return v;
    }
}
