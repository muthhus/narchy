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

import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static jcog.map.nbhm.HijacKache.*;

/**
 * Created by me on 9/4/16.
 */
public class HijackBag<X> implements Bag<X> {

    @NotNull
    public final HijacKache<X, float[]> map;

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
    int count;


    public HijackBag(int capacity, int reprobes, BudgetMerge merge, Random random) {
        this.merge = merge;
        map = new HijacKache<X, float[]>(capacity, reprobes, random) {
            @Override
            protected void reincarnateInto(Object[] k) {
                HijackBag.this.forEach((x, v) -> {
                    int idx = putIdx(k, x, v[0]);
                    if (idx != Integer.MIN_VALUE) {
                        CAS_val(k, rectify(idx), null, v);
                    } else {
                        //lost
                    }
                });
            }
        };
    }

    private static int rectify(int idx) {
        if (idx < 0) {
            return -(idx + 1);
        } else {
            return idx;
        }
    }


    @Override
    public void clear() {
        map.clear();
    }

    @Nullable
    @Override
    public BLink<X> remove(X x) {
        float[] v = map.remove(x); //<- probably works
        return (v != null) ? linkSnapshot(x, v) : null;
    }

    /**
     * returns the target array if insertion was successful, null otherwise
     */
    @Nullable
    private final float[] putBag(@NotNull final Object key, float newPri) {

        Object[] kvs = map.data;

        int idx = putIdx(kvs, key, newPri);

        if (idx == Integer.MIN_VALUE)
            return null;

        boolean gotExisting = (idx < 0);
        idx = rectify(idx);

        Object V = val(kvs, idx);         // Get old value (before volatile read below!)
        if (gotExisting) {
            if (V instanceof float[]) {
                return (float[]) V;
            } else {
                return new float[]{0, 0}; //reset?
            }
        } else {
            float[] ff = {Float.NaN, 0};
            if (CAS_val(kvs, idx, V, ff)) {
                return ff;
            } else {
                //onRemoved(linkSnapshot((X) key(kvs,idx), new float[] {0,0} /* N/A */ ));
                //return (float[]) val(kvs, idx);
                return null; //hijack got hijacked
            }
        }
    }

    /**
     * @return Integer.MIN_VALUE - unable to insert
     * < 0 : existing key found, so negate this value and add 1 to get its index
     * >= 0 : new insertion index
     */
    private int putIdx(Object[] kvs, @NotNull Object newKey, float newPri) {
        int maxReprobes = map.reprobes;

        int reprobe = 0;
        final int newKeyHash = HijacKache.hash(newKey); // throws NullPointerException if key null
        final int len = HijacKache.len(kvs); // Count of key/value pairs, reads kvs.length
        final int[] hashes = HijacKache.hashes(kvs); // Reads kvs[1], read before kvs[0]
        int idx = newKeyHash & (len - 1);

        int weakestIdx = Integer.MIN_VALUE;
        float weakestPri = Float.POSITIVE_INFINITY;

        while (true) {             // Spin till we get a Key slot

            // Found an empty Key slot - which means this Key has never been in
            // this table.  No need to put a Tombstone - the Key is not here!

            if (CAS_key(kvs, idx, null, newKey)) {
                // Claim the null key-slot
                //if (CAS_key(kvs, idx, null, key)) { // Claim slot for Key
                //chm._slots.add(1);      // Raise key-slots-used count
                hashes[idx] = newKeyHash; // Memoize fullhash
                break;                  // Got a null entry
                //} /*else {
                //K = key(kvs, idx); //recalculte
                //}*/
            }

            Object oldKey = key(kvs, idx);         // Get current key

            if (oldKey != null && keyeq(oldKey, newKey, hashes, idx, newKeyHash)) {
                return -(idx) - 1; //use negative value minus one to signal the existing entry was found
            }

            if (weakestPri != Float.NEGATIVE_INFINITY) { //if havent found an ultimate weakest index in this loop, test this one
                Object V = val(kvs, idx);
                float p = Float.NEGATIVE_INFINITY;
                if (V instanceof float[]) {
                    float[] v = (float[]) V;
                    float pv = v[0];
                    if (pv == pv) /* deleted */ {
                        p = pv;
                    }
                }

                if (p < weakestPri) {
                    weakestIdx = idx;
                    weakestPri = p;
                }

            }


            //URGENT HIJACK
            if (reprobe++ > maxReprobes) {
                //probe expired on a non-empty index,
                // attempt hijack of probed index at random,
                // erasing the old value of another key

                idx = weakestIdx; //(startIdx + rng.nextInt(maxReprobes)) & (len - 1);
                if (weakestIdx == Integer.MIN_VALUE)
                    throw new RuntimeException("no weakest found to take after probing");


                boolean hijack =
                        //hijackGreedy(newPri, weakestPri);
                        hijackSoftmax(newPri, weakestPri);

                if (hijack) {
                    if (CAS_key(kvs, idx, oldKey, newKey)) { // Got it!
                        hashes[idx] = newKeyHash; // Memoize fullhash

                        Object V = val(kvs, idx);
                        if (V instanceof float[]) {
                            float[] f = (float[]) V;
                            onRemoved(linkSnapshot((X) oldKey, new float[]{0, 0}));
                            f[0] = Float.NaN;
                        }

                    }
                } else {
                    return Integer.MIN_VALUE; //failed insert
                }
                break;
            }

            idx = (idx + 1) & (len - 1); // Reprobe!
        }
        // End of spinning till we get a Key slot
        return idx;
    }

    private boolean hijackSoftmax(float newPri, float oldPri) {

        boolean newPriThresh = newPri > Param.BUDGET_EPSILON;
        boolean oldPriThresh = oldPri > Param.BUDGET_EPSILON;
        if (!oldPriThresh) {
            if (newPriThresh)
                return true;
            else
                return map.rng.nextBoolean(); //50/50 chance
        } else {
            return (map.rng.nextFloat() > (oldPri / (newPri + oldPri)));
        }
    }

    private static boolean hijackGreedy(float newPri, float weakestPri) {
        return weakestPri <= newPri;
    }

    @Override
    public BLink<X> put(X x, Budgeted b, float scale, @Nullable MutableFloat overflowing) {

        float nP = b.pri() * scale;
        float[] f = putBag(x, nP);
        if (f == null) {
            //rejected insert
            pressure += /*range*/(nP);
            //onRemoved(null);
            return null;

        } else {

            float pBefore = f[0];
            if (pBefore == pBefore) {
                //existing to merge with
                ArrayBLink y = new ArrayBLink(x, f);
                float overflow = merge.merge(y, b, scale);
                if (overflowing != null)
                    overflowing.add(overflow);

                pressure += range(f[0]) - pBefore;
                return y;
            } else {
                //overwite an empty or deleted entry
                float p, q;
                f[0] = p = range(nP);
                f[1] = q = b.qua();
                pressure += p;

                BLink<X> l = linkSnapshot(x, p, q);
                onAdded(l);
                return l;
            }

        }

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
        return map.setCapacity(c);
    }

    @Override
    public @Nullable BLink<X> get(Object key) {
        float[] f = map.get(key);
        return f == null ? null : linkShared((X) key, f);
    }

    @NotNull
    private BLink<X> linkSnapshot(X key, float[] f) {
        return linkSnapshot(key, f[0], f[1]);
    }

    @NotNull
    private BLink<X> linkSnapshot(X key, float p, float q) {
        return new DefaultBLink<>(key, p, q);
    }

    @NotNull
    private BLink<X> linkShared(X key, float[] f) {
        if (key instanceof Budgeted) {
            return new ArrayBLink.ArrayBLinkToBudgeted((Budgeted) key, f);
        } else {
            return new ArrayBLink<>(key, f);
        }
    }

    @Override
    public int capacity() {
        return map.capacity();
    }

    @Override
    public void forEachWhile(Predicate<? super BLink<X>> each, int n) {
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

        int batchSize = n;

        float r = curve(); //randomized threshold

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        int di = map.rng.nextBoolean() ? +1 : -1;

        //ArrayBLink<X> a = new ArrayBLink<>();

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {
            int m = ((i += di) & (c - 1)) * 2 + 2;

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access
            Object k = data[m];
            Object v = data[m + 1];

            if (k != null && v instanceof float[]) {

                float[] f = (float[]) v;


                float p = f[0];
                if (p == p) {
                    if (p >= 0) {

                        if ((r < p) || (r < p + tolerance(j, jLimit, n, batchSize, c))) {
                            if (target.test(
                                    linkSnapshot((X) k, f)))
                                        /*a.set(null, f) //wraps the re-usable arrayblink
                                    )) //creates a fresh copy from it*/ {
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
        return count;
    }

    @Override
    public void forEach(Consumer<? super BLink<X>> action) {
        ArrayBLink<X> a = new ArrayBLink();
        forEach((k, b) -> {
            action.accept(a.set(k, b));
        });
    }

    public void forEach(@NotNull BiConsumer<X, float[]> e) {
        Object[] data = map.data;
        int c = (data.length - 2) / 2;

        int j = 0;

        while (j < c) {
            int m = ((j++) & (c - 1)) * 2 + 2;

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access
            Object k = data[m];
            Object v = data[m + 1];

            if (k != null && v instanceof float[]) {
                float[] f = (float[]) v;
                float p = f[0];
                if (p == p) /* NaN? */ {
                    e.accept((X) k, f);
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
        float c = map.rng.nextFloat();
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
        return map.contains(it);
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

        forEach((X x, float[] f) -> {
            float p = f[0];

            if (p > max[0]) max[0] = p;
            if (p < min[0]) min[0] = p;
            mass[0] += p;

            count[0]++;

        });


        this.priMin = min[0];
        this.priMax = max[0];
        this.count = count[0];

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
