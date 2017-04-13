package nars.bag.impl;

import nars.Param;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static jcog.Util.clamp;

/**
 * Bag which stores items, sorted, in one array.
 * Removal policy can select items by percentile via the array index.
 * A curve function maps a probabilty distribution to an index allowing the bag
 * to choose items with certain probabilities more than others.
 * <p>
 * In theory, the curve can be calculated to emulate any potential removal policy.
 * <p>
 * Insertion into the array is a O(log(n)) insertion sort, plus O(N) to shift items (unless the array is tree-like and can avoid this cost).
 * Removal is O(N) to shift items, and an additional possible O(N) if a specific item to be removed is not found at the index expected for its current priority value.
 * <p>
 * TODO make a CurveSampling interface with at least 2 implementations: Random and LinearScanning. it will use this instead of the 'boolean random' constructor argument
 */
public class CurveBag<V> extends ArrayBag<V>  {


    @NotNull
    final CurveSampler sampler;



    public CurveBag(int initialCapacity, @NotNull CurveSampler c, @NotNull BudgetMerge mergeFunction, @NotNull Map<V, BLink<V>> map) {
        super(mergeFunction, map);
        capacity(initialCapacity);
        this.sampler = c;
    }


//    @Nullable
//    @Override
//    public BLink<V> pop() {
//        return peekNext(true);
//    }



    @Override
    protected void sort() {
        super.sort();
        sampler.commit(this);
    }

    //    public @Nullable BLink<V> peekNext(boolean remove) {
//
//        synchronized (map) {
//            while (!isEmpty()) {
//
//                int index = sampleIndex();
//
//                BLink<V> i = remove ?
//                        removeItem(index) : get(index);
//
//                if (!i.isDeleted()) {
//                    return i;
//                }
//
//                //ignore this Deleted item now that it's removed from the bag
//                //if it wasnt already removed above
//                if (!remove)
//                    remove(i.get());
//
//            }
//        }
//        return null; // empty bag
//    }


    /** simplified one item sample */
    @Nullable @Override public BLink<V> sample() {
        ensureSorted();
        synchronized (items) {
            int s = size();
            while (s > 0) {
                int i = sampleIndex(s);
                BLink<V> b = get(i);
                if (b.isDeleted()) {
                    items.removeFast(i);
                    map.remove(b.get());
                    s--;
                    //then try again
                } else {
                    return b;
                }
            }
        }
        return null;
    }



    /**
     * optimized batch fill, using consecutive array elements, also ensuring uniqueness
     * returns the instance for fluentcy
     */
    @NotNull
    @Override
    public CurveBag<V> sample(int n, @NotNull Predicate<? super BLink<V>> target) {

        ensureSorted();

        //sampleIterative(n, target);
        sampleMulti(n, target);

        return this;
    }

//    /** doesnt quite work yet; can get stuck in infinite loop  */
//    private void sampleIterative(int n, @NotNull Predicate<? super BLink<V>> target) {
//        for (int i = 0; i < n; i++) {
//
//            BLink<V> next;
//            do {
//                next = sample();
//            } while (next!=null && !target.test(next));
//
//        }
//    }

    @NotNull
    private void sampleMulti(int n, @NotNull Predicate<? super BLink<V>> target) {
        ensureSorted();
        synchronized (items) {

            int ss = size();
            if (ss == 0)
                return;

            final int begin, end;
            if (ss <= n) {
                //special case: give everything
                begin = 0;
                end = ss;
                n = ss;
            } else {
                //shift upward by the radius to be fair about what is being sampled. to start from the sample index is biased against lower ranked items because they will be selected in increasing values
                int b = sampleIndex(ss);
                if (b + n > ss) {
                    b = ss - n;
                }
                begin = b;
                end = begin + n;
            }

            BLink<V>[] l = items.array();

            for (int i = begin; i < end; i++) {
                //scan the designated subarray
                if (trySample(target, l[i]))
                    n--;
            }

            if (n > 0) {
                //scan upwrads and downwrads alternating until all items iterated
                int dones = 0;

                for (int upwards = begin - 1, downwards = end + 1; (n > 0) && (dones!=3); ) {
                    //scan upwards for any remaining
                    if (upwards >= 0) {
                        if (trySample(target, l[upwards--])) {
                            if (--n == 0)
                                break;
                        }
                    } else {
                        dones |= 1; //bit 0
                    }

                    if (downwards < ss) {
                        if (trySample(target, l[downwards++])) {
                            if (--n == 0)
                                break;
                        }
                    } else {
                        dones |= 2; //bit 1
                    }
                }

            }

        }

        return;
        //System.out.println("(of " + ss + ") select " + n + ": " + begin + ".." + end + " = " + target);
    }

    public static boolean trySample(@NotNull Predicate target, BLink b) {
        return (!b.isDeleted() && target.test(b));
    }


//    @Override
//    public final @Nullable BLink<V> sample() {
//        return peekNext(false);
//    }


    //    public static long fastRound(final double d) {
//        if (d > 0) {
//            return (long) (d + 0.5d);
//        } else {
//            return (long) (d - 0.5d);
//        }
//    }
//    


    //    /**
//     * calls overflow() on an overflown object
//     * returns the updated or created concept (not overflow like PUT does (which follows Map.put() semantics)
//     * NOTE: this is the generic version which may or may not work, or be entirely efficient in some subclasses
//     */
//    public V update(final BagTransaction<K, V> selector) {
//
//
//        if (Global.DEBUG && !isSorted()) {
//            throw new RuntimeException("not sorted");
//        }
//
//        K key = selector.name();
//        V item;
//        if (key != null) {
//            item = get(key);
//        }
//        else {
//            item = peekNext();
//        }
//
//        if (item == null) {
//            item = selector.newItem();
//            if (item == null)
//                return null;
//            else {
//                // put the (new or merged) item into itemTable
//                final V overflow = put(item);
//
//                if (overflow != null)
//                    selector.overflow(overflow);
//                else if (overflow == item)
//                    return null;
//
//
//                return item;
//            }
//        } else {
//
//
//            remove(item.name());
//
//            final V changed = selector.update(item);
//
//
//            if (changed == null) {
//
//                put(item);
//
//                return item;
//            }
//            else {
//                //it has changed
//
//
//                final V overflow = put(changed);
//
//                /*if (overflow == changed)
//                    return null;*/
//
//                if (overflow != null) // && !overflow.name().equals(changed.name()))
//                    selector.overflow(overflow);
//
//                return changed;
//            }
//        }
//
//
//    }

//
//    /**
//     * optimized peek implementation that scans the curvebag
//     * iteratively surrounding a randomly selected point
//     */
//    @Override
//    protected int peekNextFill(BagSelector<K, V> tx, V[] batch, int bstart, int len, int maxAttempts) {
//
//
//        int siz = size();
//        len = Math.min(siz, len);
//
//        List<V> a = arrayBag.items.getList();
//
//        int istart;
//
//        if (len != siz) {
//            //asking for some of the items
//            int r = Math.max(1, len / 2);
//            int center = sample();
//            istart = center + r; //scan downwards from here (increasing pri order)
//
//            if (r % 2 == 1) istart--; //if odd, give extra room to the start (higher priority)
//
//            //TODO test and if possible use more fair policy that accounts for clipping
//            if (istart-r < 0) {
//                istart -= (istart - r); //start further below
//            }
//            if (istart >= siz)
//                istart = siz-1;
//        }
//        else {
//            //optimization: asking for all of the items (len==siz)
//            //   just add all elements, so dont sample
//            istart = siz-1;
//        }
//
//
//        List<K> toRemove = null;
//
//        UnitBudget b = new UnitBudget(); //TODO avoid creating this
//
//
//        //int bend = bstart + len;
//        int next = bstart;
//
//        //scan increasing priority, stopping at the top or if buffer filled
//        for (int i = istart; (i >= 0) && (next < len); i--) {
//            V v = a.get(i);
//
//            if (v == null) break; //HACK wtf?
//            //throw new RuntimeException("null");
//
//            if (v.isDeleted()) {
//                if (toRemove == null) toRemove = Global.newArrayList(0); //TODO avoid creating this
//                toRemove.add(v.name());
//            } else {
//                batch[next++] = v;
//            }
//        }
//
//        //pad with nulls. helpful for garbage collection incase they contain old values (the array is meant to be re-used)
//        if (next != len)
//            Arrays.fill(batch, bstart+next, bstart+len, null);
//
//        //update after they have been selected because this will modify their order in the curvebag
//        for (int i = bstart; i < bstart+next; i++)
//            updateItem(tx, batch[i], b);
//
//
//        if (toRemove != null)
//            toRemove.forEach(this::remove);
//
//        return next; //# of items actually filled in the array
//    }

//    public final float priAt(int cap) {
//        return arrayBag.priAt(cap);
//    }

//    //TODO
//    public int sizeQueue() {
//        return 0;
//    }


//    public BLink<V> get(int i) {
//        return arrayBag.item(i);
//    }


    /**
     * Defines the focus curve.  x is a proportion between 0 and 1 (inclusive).
     * x=0 represents low priority (bottom of bag), x=1.0 represents high priority
     *
     * @return
     */
    @FunctionalInterface
    public interface BagCurve extends FloatToFloatFunction {
    }

//    public static class RandomSampler implements ToIntFunction<CurveBag>, Serializable {
//
//        public final BagCurve curve;
//        public final Random rng;
//
//        public RandomSampler(Random rng, BagCurve curve) {
//            this.curve = curve;
//            this.rng = rng;
//        }

    /**
     * maps y in 0..1.0 to an index in [0..size). as if window=1
     */
    static int index(float y, int size) {
        return clamp(Math.round(y * (size) - 0.5f), 0, size);
    }


//    /**
//     * maps y in 0..1.0 to an index in [0..size). fairly for a window of size N (>1, <S)
//     * THIS STILL NEEDS WORK
//     */
//    static int index(float y, int size, int window) {
//
//        /** effective size for sliding around the window within the prioritization */
//        int es = (size - 1);// -  window;
//
//
//        //float slotRad = 0.5f / es;
//        int i = (int)Math.floor(y * es);// - window/2;
//
//        if (i >= es) return es;
//        if (i < 0) return 0;
//        else return i;
//    }


    public final int sampleIndex() {
        return sampleIndex(size());
    }

    private final int sampleIndex(int s) {
        return s <= 1 ? 0 : index(sampler.sample(s), s);
    }



//    /** provides a next index to sample from */
//    public final int sampleIndexUnnormalized(int s) {
//        //System.out.println("\t range:" +  min + ".." + max + " -> f(" + x + ")=" + y + "-> " + index);
//        return (s == 1) ? 0 :
//            index(
//                this.curve.valueOf( random.nextFloat()),
//                s
//            );
//    }


    /**
     * flat, gives equal attention to items in the bag
     */
    public static final BagCurve linearBagCurve = new BagCurve() {

        @Override
        public final float valueOf(float x) {
            return x;
        }

        @NotNull
        @Override
        public String toString() {
            return "LinearBagCurve";
        }

    };

    public static final BagCurve power2BagCurve = new Power2BagCurve();
    public static final BagCurve power4BagCurve = new Power4BagCurve();
    public static final BagCurve power6BagCurve = new Power6BagCurve();


    /**
     * LERPs between the curve and a flat line (y=x) in proportion to the amount the dynamic range falls short of 1.0;
     * this is probably equivalent to a naive 1st-order approximation of a way
     * to convert percentile to probability but it should suffice for now
     */
    public static final class NormalizedSampler extends CurveSampler {

        private static final float MIN_DYNAMIC_RANGE = /*(float) Math.sqrt*/(Param.BUDGET_EPSILON) /* heuristic */;
        private float range;

        public NormalizedSampler(CurveBag.BagCurve curve, Random random) {
            super(curve, random);
            this.range = 1f;
        }

        @Override
        protected void commit(@NotNull CurveBag bag) {
            float max = bag.priMax();
            float min = bag.priMin();
            this.range = max - min;
        }

        @Override
        float sample(int size) {


            float dynamicRange = (range);

            float uniform = random.nextFloat();
            if (dynamicRange < MIN_DYNAMIC_RANGE) {
                //if there is not enough dynamic range (variation from max to min) then just choose randomly (no curve)
                return uniform;
            } else {
                return ((1f - dynamicRange) * uniform) +
                        (dynamicRange * curve.valueOf(uniform));
            }
        }
    }

    public static final class DirectSampler extends CurveSampler {

        public DirectSampler(CurveBag.BagCurve curve, Random random) {
            super(curve, random);
        }

        @Override
        float sample(int size) {
            return this.curve.valueOf(random.nextFloat());
        }
    }

    public static abstract class CurveSampler {
        public final CurveBag.BagCurve curve;
        public final Random random;

        protected CurveSampler(CurveBag.BagCurve curve, Random random) {
            this.curve = curve;
            this.random = random;
        }

        /**
         * called at the end of each commit
         */
        protected void commit(CurveBag bag) {

        }

        abstract float sample(int size);
    }

//    public static class CubicBagCurve implements CurveBag.BagCurve {
//
//        @Override
//        public final float valueOf(float x) {
//            //1.0 - ((1.0-x)^2)
//            // a function which has domain and range between 0..1.0 but
//            //   will result in values above 0.5 more often than not.  see the curve:
//            //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLjAtKCgxLjAteCleMikiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjAsImVxIjoiMS4wLSgoMS4wLXgpXjMpIiwiY29sb3IiOiIjMDAwMDAwIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiLTEuMDYyODU2NzAzOTk5OTk5MiIsIjIuMzQ1MDE1Mjk2IiwiLTAuNDM2NTc0NDYzOTk5OTk5OSIsIjEuNjYwNTc3NTM2MDAwMDAwNCJdfV0-
//            float nx = 1.0f - x;
//            return 1.0f - (nx * nx * nx);
//        }
//
//        @NotNull
//        @Override
//        public String toString() {
//            return "CubicBagCurve";
//        }
//    }

    public static class Power4BagCurve implements BagCurve {

        @Override
        public final float valueOf(float x) {
            float nnx = x * x;
            return (nnx * nnx);
        }

        @NotNull
        @Override
        public String toString() {
            return "Power4BagCurve";
        }
    }

    public static class Power6BagCurve implements BagCurve {

        @Override
        public final float valueOf(float x) {
            /** x=0, y=0 ... x=1, y=1 */
            float nnx = x * x;
            return (nnx * nnx * nnx);
        }

        @NotNull
        @Override
        public String toString() {
            return "Power6BagCurve";
        }
    }

//    /**
//     * Approximates priority -> probability fairness with an exponential curve
//     */
//    @Deprecated
//    public static class FairPriorityProbabilityCurve implements BagCurve {
//
//        @Override
//        public final float valueOf(float x) {
//            return (float) (1.0f - Math.exp(-5.0f * x));
//        }
//
//        @Override
//        public String toString() {
//            return "FairPriorityProbabilityCurve";
//        }
//
//    }

    public static class Power2BagCurve implements BagCurve {

        @Override
        public final float valueOf(float x) {
            return (x * x);
        }

        @NotNull
        @Override
        public String toString() {
            return "QuadraticBagCurve";
        }

    }

    //    @Override
//    protected int update(BagTransaction<K, V> tx, V[] batch, int start, int stop, int maxAdditionalAttempts) {
//
//        super.update()
//        int center = this.sampler.applyAsInt(this);
//
//    }
}
