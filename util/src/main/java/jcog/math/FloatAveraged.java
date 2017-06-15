package jcog.math;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

public class FloatAveraged implements FloatSupplier {
    private final FloatSupplier src;
    public int history;

    //TODO use a primitive deque
    final FloatArrayList data;

    public static FloatSupplier averaged(FloatSupplier src, int history) {
        if (history <= 1)
            return src;
        return new FloatAveraged(src, history);
    }

    FloatAveraged(FloatSupplier src, int history) {
        assert(history > 1);
        data = new FloatArrayList(history);
        this.src = src;
        this.history = history;
    }

    @Override
    public float asFloat() {

        float s = src.asFloat();
        if (s == s) {
            if (data.size() == history)
                data.removeAtIndex(0);
            data.add(s);
            return (float) data.average();
        } else {
            return Float.NaN;
        }
    }

}
