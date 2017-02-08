package nars.bag.experimental;

import jcog.map.nbhm.HijacKache;
import nars.Param;
import nars.bag.Bag;
import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.link.ArrayBLink;
import nars.link.BLink;
import nars.link.DefaultBLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static jcog.map.nbhm.HijacKache.*;

/**
 * Created by me on 9/4/16.
 */
public class HijackBag<X> implements Bag<X> {

    @Deprecated private final Random random;
    private final int reprobes;

    final BLink<X>[] map;

    //@NotNull public final HijacKache<X, float[]> map;

    /**
     * max # of times allowed to scan through until either the next item is
     * accepted with final tolerance or gives up.
     * for safety, should be >= 1.0
     */
    private static final float SCAN_ITERATIONS = 1.1f;
    private final BudgetMerge merge;

    /**
     * pressure from outside trying to enter
     */
    public float pressure;

    public float mass;

    float priMin, priMax;

    final AtomicInteger count = new AtomicInteger(0);

    public HijackBag(int capacity, int reprobes, BudgetMerge merge, Random random) {
        this.merge = merge;
        this.map = new BLink[capacity];
        this.reprobes = reprobes;
        this.random = random;
//        map = new HijacKache<X, float[]>(capacity, reprobes, random) {
//            @Override
//            protected void reincarnateInto(Object[] k) {
//                HijackBag.this.forEach((x, v) -> {
//                    int idx = putIdx(k, x, v[0]);
//                    if (idx != Integer.MIN_VALUE) {
//                        CAS_val(k, rectify(idx), null, v);
//                    } else {
//                        //lost
//                    }
//                });
//            }
//        };
    }



    @Override
    public void clear() {
        if (!count.compareAndSet(0, 0)) {
            Arrays.fill(map, null);
        }
    }

    @Nullable protected BLink<X> update(Object x, @Nullable BLink<X> bx /* null to remove */, float scale /* -1 to remove, 0 to get only*/) {
        int hash = x.hashCode();


        boolean removeOrGet = bx == null;
        boolean remove = scale == -1;

        int target = -1;
        float targetPri = Float.POSITIVE_INFINITY;

        for (int r = 0; r < reprobes; r++) {
            int i = (hash + r) & (map.length-1);
            @NotNull BLink<X> c = map[i];
            if (c==null) {
                if (!removeOrGet) {
                    //empty, insert if not found elsewhere in the reprobe range
                    target = i;
                    targetPri = Float.NEGATIVE_INFINITY;
                }
            } else {
                if (x.equals(c.get())) {
                    //existing
                    if (removeOrGet) {

                        if (remove) {
                            map[i] = null;
                            onRemoved(c);
                            count.decrementAndGet();
                        }

                    } else {
                        float pBefore = c.priSafe(0);
                        merge.apply(c, bx, scale); //TODO overflow
                        pressure += c.priSafe(0) - pBefore;
                    }
                    return c;
                } else if (!removeOrGet) {
                    float cPri = c.priSafe(-1);
                    if (targetPri >= cPri) {
                        //select a better garget
                        targetPri = cPri;
                        target = i;
                    }
                    //continue probing; must try all
                }
            }
        }

        if (target!=-1) {
            if (targetPri==Float.NEGATIVE_INFINITY) {
                //insert to empty
                count.incrementAndGet();
                map[target] = bx;

                pressure += bx.priSafe(0);

                onAdded(bx);

                return bx;
            } else {
                if (hijackSoftmax(bx.priSafe(0), targetPri)) {
                    BLink<X> hijacked = map[target];
                    map[target] = bx;

                    pressure += Math.max(0, bx.priSafe(0) - hijacked.priSafe(0));

                    onRemoved(hijacked);
                    onAdded(bx);

                    return bx; //inserted
                } else {
                    return null;
                }
            }
        } else {
            return null; //removal not found
        }
    }

    @Nullable
    @Override
    public BLink<X> remove(X x) {
        return update(x, null, -1);
    }


    private boolean hijackSoftmax(float newPri, float oldPri) {

        boolean newPriThresh = newPri > Param.BUDGET_EPSILON;
        boolean oldPriThresh = oldPri > Param.BUDGET_EPSILON;
        if (!oldPriThresh) {
            if (newPriThresh)
                return true;
            else
                return random.nextBoolean(); //50/50 chance
        } else {
            return (random.nextFloat() > (oldPri / (newPri + oldPri)));
        }
    }

    private static boolean hijackGreedy(float newPri, float weakestPri) {
        return weakestPri <= newPri;
    }

    @Override
    public BLink<X> put(X x, Budgeted b, float scale, @Nullable MutableFloat overflowing) {
        BLink<X> bb = newLink(x, null);
        bb.setBudget(b);

        BLink<X> y =  update(x, bb, scale);
        if (y!=null)
            range(y.priSafe(0));
        return y;
    }

    /**
     * considers if this priority value stretches the current min/max range
     * this value will be updated for certain during a commit, so this value
     * only improves accuracy between commits.
     */
    private final float range(float p) {
        if (p > priMax) priMax = p;
        if (p < priMin) priMin = p;
        return p;
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
        //TODO
        return false;
    }

    @Override
    public @Nullable BLink<X> get(Object key) {
        return update(key, null, 0);
    }

    @Override
    public int capacity() {
        return map.length;
    }

    @Override
    public void forEachWhile(Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @NotNull
    @Override
    public Bag<X> sample(int n, Predicate<? super BLink<X>> target) {
        BLink[] data = this.map;
        int c = data.length;
        int jLimit = (int) Math.ceil(c * SCAN_ITERATIONS);

        int start = random.nextInt(c); //starting index
        int i = start, j = 0;

        int batchSize = n;

        float r = curve(); //randomized threshold

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        int di = random.nextBoolean() ? +1 : -1;

        //ArrayBLink<X> a = new ArrayBLink<>();

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {
            int m = ((i += di) & (c - 1));

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access
            BLink<X> v = data[m];

            if (v != null) {

                float p = v.pri();
                if (p == p) {
                    if (p >= 0) {

                        if ((r < p) || (r < p + tolerance(j, jLimit, n, batchSize, c))) {
                            if (target.test(v)) {
                                n--;
                                r = curve();
                            }
                        }
                    }
                } else {
                    //early deletion nullify
                    data[m] = null;
                }
            }
            j++;
        }
        return this;
    }


    @Override
    public int size() {
        return count.get();
    }

    public void forEach(@NotNull Consumer<? super BLink<X>> e) {
        BLink[] data = map;
        int c = data.length;

        int j = 0;

        while (j < c) {
            int m = ((j++) & (c - 1));

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access

            BLink<X> v = data[m];

            if (v!=null) {
                float p = v.pri();
                if (p == p) /* NaN? */ {
                    e.accept(v);
                } else {
                    data[m] = null; //nullify
                }
            }

        }
    }

    /**
     * yields the next threshold value to sample against
     */
    public float curve() {
        float c = random.nextFloat();
        c *= c; //c^2 curve

        //float min = this.priMin;
        return (c); // * (priMax - min);
    }

    /**
     * beam width (tolerance range)
     * searchProgress in range 0..1.0
     */
    private static float tolerance(int j, int jLimit, int b, int batchSize, int cap) {

        float searchProgress = ((float) j) / jLimit;
        //float selectionRate =  ((float)batchSize)/cap;

        /* raised polynomially to sharpen the selection curve, growing more slowly at the beginning */
        float exp = 6;
        return (float) Math.pow(searchProgress, exp);// + selectionRate;
    }

    /**
     * WARNING may not work correctly
     */
    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
        //return Iterators.transform(map.entryIterator(), x -> (BLink<X>) new ArrayBLink<>(x.getKey(), x.getValue()));
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(X it) {
        return get(it)!=null;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }


    @NotNull
    @Override
    public Bag<X> commit(Function<Bag, Consumer<BLink>> update) {
        final float[] mass = {0};

        final int[] count = {0};
        final float[] min = {Float.POSITIVE_INFINITY};
        final float[] max = {Float.NEGATIVE_INFINITY};

        forEach((BLink<X> f) -> {
            float p = f.priSafe(0);

            if (p > max[0]) max[0] = p;
            if (p < min[0]) min[0] = p;
            mass[0] += p;

            count[0]++;

        });


        this.priMin = min[0];
        this.priMax = max[0];

        this.mass = mass[0];

//        Forget f;
//        if (existingMass > 0 && pressure > 0) {
//            float p = this.pressure;
//            f = Forget.forget(p, existingMass, count[0], Param.BAG_THRESHOLD);
//        } else {
//            f = null;
//        }


        update(update != null ? update.apply(this) : null);

        this.pressure = 0; //HACK clear this when it is read, not after the update

        return this;
    }

    @NotNull
    public Bag<X> update(@Nullable Consumer<BLink> each) {
        if (each != null && !isEmpty())
            forEach(each);

        return this;
    }


    @Override
    public BLink<X> mul(@NotNull Object key, float factor) {
        BLink<X> b = get(key);
        if (b != null && !b.isDeleted()) {
            float before = b.pri();
            b.priMult(factor);
            float after = range(b.pri());
            pressure += (after - before);
            return b;
        }
        return null;
    }

    @Override
    public BLink<X> add(@NotNull Object key, float x) {
        BLink<X> b = get(key);
        if (b != null && !b.isDeleted()) {
            float before = b.pri();
            b.priAdd(x);
            float after = range(b.pri());
            pressure += (after - before);
            return b;
        }
        return null;
    }


}
