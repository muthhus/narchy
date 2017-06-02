package jcog.math;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

public class FloatAveraged implements FloatSupplier {
    private final FloatSupplier src;
    public int history;

    //TODO use a primitive deque
    final FloatArrayList data;

    public FloatAveraged(FloatSupplier src, int history) {
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
