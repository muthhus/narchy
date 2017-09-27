package jcog.pri;

import jcog.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/** groups a set of items into one link which are selected from
 * on .get() invocations.
 * a transducer is applied so that a type transformation
 * of the input can allow shared inputs to multiple different link output types.
 */
public class MultiLink<X extends Prioritized,Y> extends AbstractPLink<Y> {

    private final int hash;
    private final X[] x;
    private final Function<X, Y> transduce;

    public MultiLink(@NotNull X[] x, Function<X,Y> transduce, float p) {
        super(p);
        this.x = x;
        this.transduce = transduce;
        hash = Util.hashCombine(transduce.hashCode(), x.hashCode());
    }

    @Override
    public boolean equals(@NotNull Object that) {
        return this == that || hash==that.hashCode() || (that instanceof MultiLink && ((MultiLink)that).transduce.equals(transduce) && Arrays.equals(x, ((MultiLink)that).x));
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Nullable
    @Override
    public Y get() {
        float priSum = 0;
        int deleted = 0;
        int c = x.length;
        for (int i = 0; i < c; i++) {
            float p = x[i].pri();
            if (p!=p) {
                deleted++;
            } else {
                priSum += p;
            }
        }
        if (deleted == c) {
            super.delete();
            return null;
        }

        int s = Util.decideRoulette(c, i -> x[i].priElseZero(), ThreadLocalRandom.current());
        return transduce.apply(x[s]);
    }

    @Override
    public boolean delete() {
        return false;
    }

}
