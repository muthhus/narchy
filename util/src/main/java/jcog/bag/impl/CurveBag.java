package jcog.bag.impl;

import jcog.Util;
import jcog.pri.Pri;
import jcog.pri.Prioritized;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

/**
 * ArrayBag with a randomized sampling range
 */
public class CurveBag<X extends Prioritized> extends PriArrayBag<X> {

    private final Random random;

    public CurveBag(@NotNull PriMerge mergeFunction, @NotNull Map<X, X> map, Random rng, int cap) {
        this(mergeFunction, map, rng);
        setCapacity(cap);
    }


    public CurveBag(@NotNull PriMerge mergeFunction, @NotNull Map<X, X> map, Random rng) {
        super(mergeFunction, map);
        this.random = rng;
    }

    @Override
    protected int sampleStart(int size) {
        if (size == 1)
            return 0;
        else {
            float min = this.min;
            float max = this.max;
            float diff = max - min;
            if (diff > Pri.EPSILON * size) {
                float i = random.nextFloat(); //uniform
                //normalize to the lack of dynamic range
                i = Util.lerp(diff, i /* flat */, (i*i) /* curved */);
                int j = (int)(i * (size-0.5f));
                if (j == size) j--;
                return j;
            } else {
                return random.nextInt(size);
            }
        }
    }

    @Override
    protected Random random() {
        return random;
    }


    //    /** optimized point sample impl */
//    @Nullable
//    @Override public PriReference<X> sample() {
//        Object[] ii = items.array();
//        if (ii.length == 0)
//            return null;
//
//        int size = Math.min(ii.length, this.size());
//        if (size == 0)
//            return null;
//
//        if (size == 1)
//            return (PriReference<X>) ii[0];
//
//        for (int i = 0; i < size /* max # of trials */; i++) {
//            Object n = ii[ThreadLocalRandom.current().nextInt(size)];
//            if (n != null)
//                return (PriReference<X>) n;
//        }
//        return null;
//    }


}