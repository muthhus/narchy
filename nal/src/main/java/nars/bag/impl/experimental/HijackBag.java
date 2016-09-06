package nars.bag.impl.experimental;

import nars.Param;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.Forget;
import nars.budget.UnitBudget;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.util.data.map.nbhm.HijacKache;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.util.Util.clamp;

/**
 * Created by me on 9/4/16.
 */
public class HijackBag<X> implements Bag<X> {

    public final HijacKache<X, BLink<X>> map;

    /**
     * max # of times allowed to scan through until either the next item is
     * accepted with final tolerance or gives up.
     * for safety, should be >= 1.0
     */
    private static final float SCAN_ITERATIONS = 1.1f;

    private float pressure = 0;
    float priMin, priMax;


    /**
     * the fraction of capacity which must contain entries to exceed in order to apply forgetting.
     * this is somewhat analogous to hashmap load factor
     */
    private float FORGET_CAPACITY_THRESHOLD = 0.75f;


    public HijackBag(int capacity, int reprobes) {
        this(capacity, reprobes, new XorShift128PlusRandom(1));
    }

    public HijackBag(int capacity, int reprobes, Random random) {
        map = new HijacKache<>(capacity, reprobes, random);
    }


    @Override
    public void clear() {
        map.clear();
    }

    @Nullable
    @Override
    public BLink<X> remove(X x) {
        return map.remove(x);
    }

    @Override
    public void put(X x, Budgeted b, float scale, MutableFloat overflowing) {

        float dPending = 0;

        BLink prev = map.computeIfAbsent(x, (xx) -> newLink(xx, UnitBudget.Zero));

        if (x.equals(prev.get())) {
            //link exists, merge with it

            float pBefore = prev.pri();

            float overflow = BudgetMerge.plusBlend.merge(prev, b, scale);
            if (overflowing != null)
                overflowing.add(overflow);

            float pAfter = prev.pri();
            dPending = pAfter - pBefore;

            range(pAfter);

        } else {
//            BLink next = (BLink) newLink(x, b).priMult(scale);
//
//            BLink<X> prev = put(x, next);
//
            float np = b.pri() * scale;


            float dp = prev.pri();
            if (dp == dp) { //not deleted, compare the two
                float den = np + dp;
                float probNext = den > Param.BUDGET_EPSILON ? np / den : 0.5f;

                if (map.rng.nextFloat() > probNext) {
                    //keep the old value
                    dPending = 0;
                } else {
                    //use the new value
                    float pBefore = prev.priIfFiniteElseZero();
                    prev.set(x, b, scale);
                    float pAfter = prev.pri();
                    range(pAfter);
                    dPending = pAfter - pBefore;
                }

            } else {
                prev.set(x, b, scale);
                dPending = np;
            }

        }

        pressure += dPending;

    }

    /**
     * considers if this priority value stretches the current min/max range
     */
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
    public boolean setCapacity(int c) {
        return map.setCapacity(c);
    }

    @Override
    public @Nullable BLink<X> get(Object key) {
        return map.get(key);
    }

    @Override
    public int capacity() {
        return map.capacity();
    }

    @Override
    public void topWhile(Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @NotNull
    @Override
    public Bag<X> sample(int n, Predicate<? super BLink<X>> target) {
        Object[] data = this.map.data;
        int c = (data.length - 2) / 2;
        int jLimit = (int) Math.ceil(c * SCAN_ITERATIONS);

        int start = map.rng.nextInt(c); //starting index
        int i = start, j = 0;

        float r = curve(); //randomized threshold

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        int di = map.rng.nextBoolean() ? +1 : -1;

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {
            Object v = data[((i += di) & (c - 1)) * 2 + 3];

            if (v instanceof BLink) {
                BLink<X> x = (BLink<X>) v;
                float p = x.priIfFiniteElseNeg1();
                if (p >= 0) {
                    if ((r < p) || (r < p + tolerance((((float) j) / jLimit)))) {
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

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void forEach(Consumer<? super BLink<X>> action) {
        Object[] data = map.data;
        int c = (data.length - 2) / 2;

        int j = 0;

        while (j < c) {
            Object v = data[((j++) & (c - 1)) * 2 + 3];

            if (v instanceof BLink) {
                action.accept((BLink<X>) v);
            }
        }
    }

    /**
     * yields the next threshold value to sample against
     */
    public float curve() {
        float c = map.rng.nextFloat();
        c *= c; //c^2 curve

        //float min = this.priMin;
        return (c); // * (priMax - min);
    }

    /**
     * beam width (tolerance range)
     * searchProgress in range 0..1.0
     */
    private float tolerance(float searchProgress) {
        /* raised polynomially to sharpen the selection curve, growing more slowly at the beginning */
        float exp = 6;
        return (float) Math.pow(searchProgress, exp);
    }

    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
        return map.values().iterator();
    }

    @Override
    public boolean contains(X it) {
        return map.contains(it);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
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

        Iterator<Map.Entry<X, BLink<X>>> es = map.entrySet().iterator();
        while (es.hasNext()) {
            Map.Entry<X, BLink<X>> e = es.next();
            Object ll = e.getValue();
            if (!(ll instanceof BLink))
                continue; //may be an Integer 'ticket' being computed, skip

            BLink<X> l = (BLink<X>)ll;

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

            float forgetRate = clamp(p / (p + mass));

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
        if (each != null && !isEmpty())
            forEach(each);

        return this;
    }


    @Override
    public X boost(Object key, float boost) {
        BLink<X> b = get(key);
        if (b != null && !b.isDeleted()) {
            float before = b.pri();
            b.priMult(boost);
            float after = b.pri();
            pressure += (after - before);
            return b.get();
        }
        return null;
    }
}
