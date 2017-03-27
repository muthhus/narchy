package jcog.bag.impl;

import jcog.Util;
import jcog.bag.Bag;
import jcog.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by me on 2/17/17.
 */
public abstract class HijackBag<K, V> implements Bag<K, V> {

    public static final AtomicReferenceArray EMPTY_ARRAY = new AtomicReferenceArray(0);

    /**
     * max # of times allowed to scan through until either the next item is
     * accepted with final tolerance or gives up.
     * for safety, should be >= 1.0
     */
    private static final float SCAN_ITERATIONS = 1f;
    protected final Random random;
    public final int reprobes;
    public transient final AtomicReference<AtomicReferenceArray<V>> map;
    final AtomicInteger size = new AtomicInteger(0);
    final AtomicInteger capacity = new AtomicInteger(0);

    /**
     * ID of this bag, for use in constructing keys for the global treadmill
     */
    private final int id;


    /**
     * lock-free int -> int mapping used as a ticket barrier
     */
    static final class Treadmill {

        static final ConcurrentLongSet map = new ConcurrentLongSet(Util.MAX_CONCURRENCY * 2);

        static final AtomicInteger ticket = new AtomicInteger(0);

        public static int newTarget() {
            return ticket.incrementAndGet();
        }

        public static long start(int target, int hash) {

            long ticket = (((long) target) << 32) | hash;

            map.putIfAbsentRetry(ticket);

            return ticket;
        }

        public static void end(long ticket) {
            map.remove(ticket);
        }
    }


    /**
     * pressure from outside trying to enter
     */
    public float pressure;
    public float mass;
    float priMin;
    float priMax;

    public HijackBag(int initialCapacity, int reprobes, Random random) {
        this(reprobes, random);
        setCapacity(initialCapacity);
    }

    public HijackBag(int reprobes, Random random) {
        this.random = random;
        this.reprobes = reprobes;
        this.id = Treadmill.newTarget();
        this.map = new AtomicReference<>(EMPTY_ARRAY);
    }

    private static int i(int c, int hash) {
        return (int) (Integer.toUnsignedLong(hash) % c);
    }

    public static boolean hijackGreedy(float newPri, float weakestPri) {
        return weakestPri <= newPri;
    }

    public static <X, Y> void forEachActive(@NotNull HijackBag<X, Y> bag, @NotNull Consumer<? super Y> e) {
        forEachActive(bag, bag.map.get(), e);
    }

    public static <X, Y> void forEachActive(@NotNull HijackBag<X, Y> bag, @NotNull AtomicReferenceArray<Y> map, @NotNull Consumer<? super Y> e) {
        forEach(map, bag::active, e);
    }

    @Override
    public void pressurize(float f) {
        pressure += f;
    }

    public static <Y> void forEach(@NotNull AtomicReferenceArray<Y> map, @NotNull Predicate<Y> accept, @NotNull Consumer<? super Y> e) {
        for (int c = map.length(), j = -1; ++j < c; ) {
            Y v = map.get(j);
            if (v != null && accept.test(v)) {
                e.accept(v);
            }
        }
    }

    public static <Y> void forEach(AtomicReferenceArray<Y> map, @NotNull Consumer<? super Y> e) {
        for (int c = map.length(), j = -1; ++j < c; ) {
            Y v = map.get(j);
            if (v != null) {
                e.accept(v);
            }
        }
    }

    @Override
    public final boolean setCapacity(int _newCapacity) {

        int newCapacity = Math.max(_newCapacity, reprobes);

        if (capacity.getAndSet(newCapacity) != newCapacity) {

            List<V> removed = new FasterList<>();

            final AtomicReferenceArray<V>[] prev = new AtomicReferenceArray[1];

            //ensures sure only the thread successful in changing the map instance is the one responsible for repopulating it,
            //in the case of 2 simultaneous threads deciding to allocate a replacement:
            AtomicReferenceArray<V> next = newCapacity != 0 ? new AtomicReferenceArray<V>(newCapacity) : EMPTY_ARRAY;
            if (next == this.map.updateAndGet((x) -> {
                if (x.length() != newCapacity) {
                    prev[0] = x;
                    return next;
                } else return x;
            })) {
                //copy items from the previous map into the new map. they will be briefly invisibile while they get transferred.  TODO verify
                forEachActive(this, prev[0], (b) -> {
                    size.decrementAndGet(); //decrement size in any case. it will be re-incremented if the value was accepted during the following put
                    if (put(b) == null) {
                        removed.add(b);
                    }
                });
            }

            removed.forEach(this::onRemoved);

            return true;
        }

        return false;
    }

    @Override
    public void clear() {
        AtomicReferenceArray<V> x = reset();
        if (x != null) {
            forEachActive(this, x, this::onRemoved);
        }
    }

    @Nullable
    public AtomicReferenceArray<V> reset() {
        if (!size.compareAndSet(0, 0)) {
            AtomicReferenceArray<V> prevMap = map.getAndSet(new AtomicReferenceArray<V>(capacity()));
            pressure = 0;
            this.priMax = 0;
            this.priMin = 0;
            return prevMap;
        } else {
            return null;
        }
    }

    @Nullable
    protected V update(Object x, @Nullable V adding /* null to remove */, float scale /* -1 to remove, 0 to get only*/) {

        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;


        boolean add = adding != null;
        boolean remove = (!add && (scale == -1));

        V added = null;

        V found = null; //get or remove

        boolean merged = false;


        int targetIndex = -1;
        V target = null;

        boolean dir = random.nextBoolean(); //choose random initial direction

        int hash = x.hashCode();
        int iStart = i(c, hash);

        //final long ticket = add ? Treadmill.start(id, hash) : Long.MIN_VALUE /* N/A for get or remove */;

        try {

            for (int retry = 0; retry < reprobes; retry++, dir = !dir) {

                float targetPri = Float.POSITIVE_INFINITY;
                int i = dir ? iStart : (iStart + reprobes) - 1;

                for (int probe = 0; probe < reprobes; probe++) {

                    if (i >= c) i -= c;
                    else if (i < 0) i += c;

                    V ii = map.get(i);

                    if (ii == null) {
                        if (add && targetPri != Float.NEGATIVE_INFINITY) {
                            //empty, plan to take this if
                            // another empty has not been planned
                            // and only take it if the value has not been found elsewhere in the reprobe range, entirely scanned
                            target = null;
                            targetIndex = i;
                            targetPri = Float.NEGATIVE_INFINITY;
                        }
                    } else {
                        K y = key(ii);
                        if (y == x || y.equals(x)) { //existing

                            if (!add) {

                                if (remove) { //remove
                                    if (map.compareAndSet(i, ii, null)) {
                                        found = ii;
                                    }
                                } else {
                                    found = ii; //get
                                }

                            } else { //put
                                pressure += merge(ii, adding, scale);

                                targetIndex = -1;
                                added = ii;
                                merged = true;
                            }

                            break; //continue below

                        } else if (add) {
                            float iiPri = priSafe(ii, -1);
                            if (targetPri > iiPri) {
                                //select a better target
                                target = ii;
                                targetIndex = i;
                                targetPri = iiPri;
                            }
                            //continue probing; must try all
                        }
                    }

                    i = dir ? (i + 1) : (i - 1);
                }

                //add at target index
                if (targetIndex != -1) {

                    if (targetPri == Float.NEGATIVE_INFINITY) {

                        //insert to empty
                        if (map.compareAndSet(targetIndex, null, adding)) {
                            pressure += merge(null, adding, scale);
                            added = adding;
                        }

                    } else {
                        if (replace(adding, target)) {
                            if (map.compareAndSet(targetIndex, target, adding)) { //inserted
                                //pressure -= targetPri;
                                pressure += merge(null, adding, scale);
                                found = target;
                                added = adding;
                            }
                        }
                    }
                }

                if (!add || (added != null))
                    break;

                //else: try again
            }
        } catch (Throwable t) {
            t.printStackTrace(); //should not happen
        }

//        if (add) {
//            Treadmill.end(ticket);
//        }

        if (!merged) {
            if (found != null && (add || remove)) {
                size.decrementAndGet();
                onRemoved(found);
            }

            if (added != null) {
                size.incrementAndGet();
                onAdded(added);
            } else if (add) {
                //rejected: add but not added and not merged
                pressure += pri(adding) * scale;
            }


        }

//        /*if (Param.DEBUG)*/
//        {
//            int cnt = size.get();
//            if (cnt > c) {
//                //throw new RuntimeException("overflow");
//            } else if (cnt < 0) {
//                //throw new RuntimeException("underflow");
//            }
//        }


        return !add ? found : added;

    }


    /**
     * if no existing value then existing will be null.
     * this should modify the existing value if it exists,
     * or the incoming value if none.
     *
     * @return the pressure increase the merge causes.
     * <p>
     * NOTE:
     * this should usually equal the amount of priority increased by the
     * insertion (considering the scale's influence too) as if there were
     * no existing budget to merge with, even if there is.
     * <p>
     * this supports fairness so that existing items will not have a
     * second-order budgeting advantage of not contributing as much
     * to the presssure as new insertions.
     */
    protected abstract float merge(@Nullable V existing, @NotNull V incoming, float scale);

    /**
     * can override in subclasses for custom replacement policy.
     * true allows the incoming to replace the existing
     */
    protected boolean replace(V incoming, V existing) {
        float incomingPri = pri(incoming);
        return hijackSoftmax(incomingPri, pri(existing));
    }

    /**
     * can override in subclasses for custom equality test
     */
    protected boolean equals(Object x, K y) {
        return x.equals(y);
    }

    @Nullable
    @Override
    public V remove(@NotNull K k) {
        return update(k, null, -1);
    }

    protected boolean hijackSoftmax(float newPri, float oldPri) {

        float priEpsilon = priEpsilon();

        if (oldPri > priEpsilon) {
            return random.nextFloat() > (oldPri / ((newPri / reprobes) + oldPri));
        } else {
            return (newPri > priEpsilon) || random.nextFloat() > 0.5f / reprobes;
            // random.nextBoolean(); //50/50 chance
        }
    }

    /**
     * warning: the instance 'bb' that is passed here may be modified by this
     */
    @Override
    public V put(@NotNull V bb, float scale, /* TODO */ @Nullable MutableFloat overflowing) {

        V y = update(key(bb), bb, scale);

        if (y != null)
            startsRange(priSafe(y, 0));

        return y;
    }


    /**
     * considers if this priority value stretches the current min/max range
     * this value will be updated for certain during a commit, so this value
     * only improves accuracy between commits.
     */
    private float startsRange(float p) {
        if (p != p)
            throw new RuntimeException("NaN prioritization");

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
    public @Nullable V get(@NotNull Object key) {
        return update(key, null, 0);
    }

    @Override
    public int capacity() {
        return capacity.get();
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super V> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @Nullable
    @Override
    public HijackBag<K, V> sample(int n, @NotNull Predicate<? super V> target) {
        scan(n, false, target);
        return this;
    }

    @Nullable
    @Override
    public int pop(int n, @NotNull Predicate<? super V> target) {
        return scan(n, true, target);
    }

    /**
     * @return how many items selected/removed
     */
    public int scan(int n, boolean remove, @NotNull Predicate<? super V> target) {
        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return 0;

        /*if (isEmpty())
            return null;*/

        int jLimit = (int) Math.ceil(c * SCAN_ITERATIONS);

        int i = random.nextInt(c), j = 0;


        float min = priMin;
        float max = priMax;
        float priRange = max - min;

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        boolean di = random.nextBoolean();
        int selected = 0;

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {

            if (di) {
                if (++i == c) i = 0;
            } else {
                if (--i == -1) i = c - 1;
            }

            j++;

            V v = map.get(i);
            if (v == null) {
                continue;
            }

            float p = pri(v);
            if (p == p) {

                float r = curve() * priRange + priMin; //randomized threshold

                if (r < p + tolerance(((float) j) / jLimit)) { /*|| (r < p + tolerance(j, jLimit, n, batchSize, c))*/
                    if (!remove || map.compareAndSet(i, v, null)) {
                        if (target.test(v)) {
                            if (remove) {
                                size.decrementAndGet();
                            }
                            n--;
                            selected++;
                        } else {
                            //try to reinsert in that slot
                            if (!map.compareAndSet(i, null, v)) {
                                //try to insert as if normally
                                put(v);
                            }
                        }
                    }
                }

            } else {
                //early deletion nullify
//                    if (map.compareAndSet(m, v, null)) {
//                        size.decrementAndGet();
//                        onRemoved(v);
//                    }
            }


        }

        return selected;
    }


    /**
     * a value between 0 and 1 of how much percent of the priority range
     * to accept an item which is below the current sampling target priority.
     * generally this should be a monotonically increasing function of
     * the scan progress proportion, a value in 0..1.0 also.
     */
    protected static float tolerance(float scanProgressProportion) {
        return /*Util.sqr*/(Util.sqr(scanProgressProportion)); /* polynomial curve */
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public void forEach(@NotNull Consumer<? super V> e) {
        forEachActive(this, e);
    }

    /**
     * yields the next threshold value to sample against
     */
    public float curve() {
        float c = random.nextFloat();
        return 1f - (c * c);
    }

    @Override
    public Spliterator<V> spliterator() {
        return stream().spliterator();
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return stream().iterator();
    }

    public Stream<V> stream() {
        AtomicReferenceArray<V> map = this.map.get();
        return IntStream.range(0, map.length()).mapToObj(map::get).filter(Objects::nonNull);
    }

    @Override
    public boolean contains(@NotNull K it) {
        return get(it) != null;
    }

    @Override
    @Deprecated
    public Bag<K, V> commit() {
        float p = this.pressure;
        if (p > 0) {
            pressure = 0;
            return commit(Bag.forget(size(), p, mass, temperature(), priEpsilon(), this::forget));
        }
        return this;
    }

    /**
     * higher value means faster forgetting
     */
    protected float temperature() {
        return 0.5f;
    }

    protected float priEpsilon() {
        return Float.MIN_VALUE;
    }

    protected abstract Consumer<V> forget(float rate);

    @NotNull
    @Override
    public HijackBag<K, V> commit(@Nullable Consumer<V> update) {

        if (update!=null) {
            update(update);
        }

        float mass = 0;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        int count = 0;

        AtomicReferenceArray<V> a = map.get();
        for (int i = 0; i < a.length(); i++) {
            V f = a.get(i);
            if (f != null) {
                float p = priSafe(f, -1);
                if (p >= 0) {
                    count++;
                    if (p > max) max = p;
                    if (p < min) min = p;
                    mass += p;
                } else {
                    if (a.compareAndSet(i, f, null)) {
                        onRemoved(f); //TODO this may call onRemoved unnecessarily if the map has changed (ex: resize)
                    }
                }
            }
        }

        this.size.set(count);

        if (min == Float.POSITIVE_INFINITY) {
            this.priMin = 0;
            this.priMax = 0;
        } else {
            this.priMin = min;
            this.priMax = max;
        }

        this.mass = mass;

        return this;
    }

    @NotNull
    protected HijackBag<K, V> update(@Nullable Consumer<V> each) {

        if (each != null) {
            forEach(each);
        }

        return this;
    }

    /**
     * SUSPECT
     */
    public static <X> Stream<X> stream(AtomicReferenceArray<X> a) {
        return IntStream.range(0, a.length()).mapToObj(a::get);//.filter(Objects::nonNull);
    }

    public static <X> List<X> list(AtomicReferenceArray<X> a) {
        return IntStream.range(0, a.length()).mapToObj(a::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /*private static int i(int c, int hash, int r) {
        return (int) ((Integer.toUnsignedLong(hash) + r) % c);
    }*/


    //    /**
//     * beam width (tolerance range)
//     * searchProgress in range 0..1.0
//     */
//    private static float tolerance(int j, int jLimit, int b, int batchSize, int cap) {
//
//        float searchProgress = ((float) j) / jLimit;
//        //float selectionRate =  ((float)batchSize)/cap;
//
//        /* raised polynomially to sharpen the selection curve, growing more slowly at the beginning */
//        return Util.sqr(Util.sqr(searchProgress * searchProgress));// * searchProgress);
//
//        /*
//        float exp = 6;
//        return float) Math.pow(searchProgress, exp);// + selectionRate;*/
//    }

}
