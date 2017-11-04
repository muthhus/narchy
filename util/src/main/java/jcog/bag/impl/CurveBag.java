package jcog.bag.impl;

import jcog.Util;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;

/**
 * ArrayBag with a randomized sampling range
 */
public class CurveBag<X extends Priority> extends PriArrayBag<X> {

    /** TODO pass random as argument to sample(..) rather than store field here */
    @Deprecated @Nullable
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
    protected int sampleStart(Random random, int size) {
        assert(size > 0);
        if (size == 1 || random==null)
            return 0;
        else {
            float min = this.min;
            float max = this.max;
            float diff = max - min;
            if (diff > Prioritized.EPSILON * size) {
                float i = random.nextFloat(); //uniform
                //normalize to the lack of dynamic range
                i = Util.lerp(diff, i /* flat */, (i*i) /* curved */);
                int j = (int) /*Math.floor*/(i * (size-0.5f));
                if (j >= size) j = size-1;
                else if (j < 0) j = 0;
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