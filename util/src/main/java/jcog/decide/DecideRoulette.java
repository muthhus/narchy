package jcog.decide;

import jcog.Util;
import jcog.list.FasterList;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class DecideRoulette<X> extends FasterList<X> {
    private final FloatFunction<X> eval;
    private float[] values;
    private float sum;

    public DecideRoulette(FloatFunction<X> value) {
        super();
        this.eval = value;
    }

    @Nullable
    public X decide(Random rng) {
        int s = size();
        if (s == 0)
            return null;
        return get(decideWhich(rng));
    }

    public int decideWhich(Random rng) {
        int s = size();
        if (s == 0)
            return -1;
        else if (s == 1)
            return 0;

        if (this.values == null || this.values.length!= s) {
            this.values = new float[s];
            float sum = 0;
            for (int i = 0; i < s; i++) {
                float f = eval.floatValueOf(get(i));
                values[i] = f;
                sum += f;
            }
            this.sum = sum;
        }
        return Util.decideRoulette(s, (i)->values[i], sum, rng);
    }
}
