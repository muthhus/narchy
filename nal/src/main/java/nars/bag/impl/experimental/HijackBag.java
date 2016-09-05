package nars.bag.impl.experimental;

import nars.Param;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.Forget;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.util.data.map.nbhm.HijacKache;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.util.Util.clamp;

/**
 * Created by me on 9/4/16.
 */
public class HijackBag<X> extends HijacKache<X,BLink<X>> implements Bag<X> {




    /** max # of times allowed to scan through until either the next item is
     *  accepted with final tolerance or gives up.
     *  for safety, should be >= 1.0
     */
    private static final float SCAN_ITERATIONS = 1.1f;

    private float pressure = 0;
    float priMin, priMax;


    /** the fraction of capacity which must contain entries to exceed in order to apply forgetting.
     * this is somewhat analogous to hashmap load factor
     * */
    private float FORGET_CAPACITY_THRESHOLD = 0.75f;


    public HijackBag(int capacity, int reprobes) {
        this(capacity, reprobes, new XorShift128PlusRandom(1));
    }

    public HijackBag(int capacity, int reprobes, Random random) {
        super(capacity, reprobes, random);
    }


    @Override
    public void put(X x, Budgeted b, float scale, MutableFloat overflowing) {



        float dPending = 0;

        BLink prev = get(x);
        if (prev!=null) {
            float pBefore = prev.pri();

            float overflow = BudgetMerge.plusBlend.merge(prev, b, scale);
            if (overflowing!=null)
                overflowing.add(overflow);

            float pAfter = prev.pri();
            dPending = pAfter - pBefore;

            range(pAfter);

        } else {
            BLink next = (BLink) newLink(x, b).priMult(scale);

            BLink<X> displaced = put(x, next);

            float np = next.pri();

            if (displaced == null) {
                dPending = np;
            } else {

                float dp = displaced.pri();
                if (dp == dp) { //not deleted, compare the two
                    float den = np + dp;
                    float probNext = den > Param.BUDGET_EPSILON ? np / den : 0.5f;

                    if (rng.nextFloat() < probNext) {
                        dPending = np - dp; //keep the new value
                        range(np);
                    } else {
                        remove(x);
                        put(displaced.get(), displaced); //reinsert what was removed
                        range(dp);
                    }

                } else {
                    dPending = np;
                }
            }
        }

        pressure += dPending;

//        BLink result = merge(i, l, (prev, next) -> {
//            //warning due to lossy overwriting, the key of next may not be equal to the key of prev
//            if (prev.get().equals(next.get())) {
//                //float pBefore = prev.pri();
//                BudgetMerge.plusBlend.apply(prev, next);
//                return prev;
//            } else {
//                float n = next.pri();
//                float probNext = n / (n + prev.pri());
//                if (rng.nextFloat() < probNext)
//                    return next; //overwrite
//                else
//                    return prev; //keep old value
//            }
//        });
    }

    /** considers if this priority value stretches the current min/max range */
    private final void range(float p) {
        if (p > priMax) priMax = p;
        if (p < priMin) priMin = p;
    }

    @Override
    public float priMin() {
        return priMin;
    }

    @Override
    public float priMax() {
        return priMax;
    }

    @Override
    public void topWhile(Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @NotNull
    @Override
    public Bag<X> sample(int n, Predicate<? super BLink<X>> target) {
        Object[] data = this.data;
        int c = (data.length - 2)/2;
        int jLimit = (int)Math.ceil(c * SCAN_ITERATIONS);

        int start = rng.nextInt(c); //starting index
        int i = start, j = 0;

        float r = curve(); //randomized threshold

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        int di = rng.nextBoolean() ? +1 : -1;

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {
            Object v = data[ ((i+=di) & (c-1))*2 + 3 ]; /* capacity may change during the loop so always get the latest value */;

            if (v instanceof BLink) {
                BLink<X> x = (BLink<X>) v;
                float p = x.priIfFiniteElseNeg1();
                if (p >= 0) {
                    if ((r < p) || (r < p + tolerance((((float)j) / jLimit)))) {
                        if (target.test(x)) {
                            n--;
                            r = curve();
                        }
                    }
                }
            }
            j++;
        }
        return this;
    }

    /** yields the next threshold value to sample against */
    public float curve() {
        float c = rng.nextFloat();
        c*=c; //c^2 curve

        //float min = this.priMin;
        return (c ); // * (priMax - min);
    }

    /**
     * beam width (tolerance range)
     * searchProgress in range 0..1.0 */
    private float tolerance(float searchProgress) {
        /* raised polynomially to sharpen the selection curve, growing more slowly at the beginning */
        float exp = 6;
        return (float)Math.pow(searchProgress,exp);
    }

    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
        return values().iterator();
    }

    @NotNull
    @Override
    public Bag<X> commit() {
//        if (isEmpty())
//            return this;

        float mass = 0;

        int count = 0;
        int cap = capacity();
        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;

        Iterator<Entry<X, BLink<X>>> es = entrySet().iterator();
        while (es.hasNext()) {
            Entry<X, BLink<X>> e = es.next();
            BLink<X> l = e.getValue();

            boolean delete = false;
            if (l.isDeleted())
                delete = true;
            X x = l.get();
            if (x == null)
                delete = true;

            if (delete) {
                es.remove();
            } else {
                float p = l.pri();
                if (p > max) max = p;
                if (p < min) min = p;
                mass += p;
                count++;
            }
        }

        this.priMin = min;
        this.priMax = max;

        Forget f;
        if (mass > 0 && (count >= cap * FORGET_CAPACITY_THRESHOLD)) {
            float p = this.pressure;
            this.pressure = 0;

            float forgetRate = clamp(p / (p+mass));

            if (forgetRate > Param.BUDGET_EPSILON) {
                f = new Forget(forgetRate);
            } else {
                f = null;
            }
        } else {
            f = null;
            this.pressure = 0;
        }

        return commit(f);
    }

    @NotNull
    @Override
    public Bag<X> commit(Consumer<BLink> each) {
        if (each!=null && !isEmpty())
            forEach(each);

        return this;
    }


    @Override
    public X boost(Object key, float boost) {
        BLink<X> b = get(key);
        if (b!=null && !b.isDeleted()) {
            float before = b.pri();
            b.priMult(boost);
            float after = b.pri();
            pressure += (after - before);
            return b.get();
        }
        return null;
    }
}
