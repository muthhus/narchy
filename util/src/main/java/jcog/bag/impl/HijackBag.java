package jcog.bag.impl;

import jcog.Util;
import jcog.bag.Bag;
import jcog.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.*;
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

    public final int reprobes;
    public transient final AtomicReference<AtomicReferenceArray<V>> map;
    final AtomicInteger capacity = new AtomicInteger(0);

    public final DoubleAdder pressure = new DoubleAdder();

    int size = 0;
    float mass;
    float priMin;
    float priMax;

    public HijackBag(int initialCapacity, int reprobes) {
        this(reprobes);
        setCapacity(initialCapacity);
    }

    public HijackBag(int reprobes) {
        this.reprobes = reprobes;
        this.map = new AtomicReference<>(EMPTY_ARRAY);
    }

    protected Random random() {
        return ThreadLocalRandom.current();
    }

    @Contract(pure = true)
    private static int i(int c, int hash) {
        //return (int) (Integer.toUnsignedLong(hash) % c);
        return Math.abs(hash) % c;
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
        pressure.add(f);
    }

    public static <Y> void forEach(@NotNull AtomicReferenceArray<Y> map, @NotNull Predicate<Y> accept, @NotNull Consumer<? super Y> e) {
        for (int c = map.length(), j = 0; j < c; j++) {
            Y v = map.getOpaque(j);
            if (v != null && accept.test(v)) {
                e.accept(v);
            }
        }
    }

    public static <Y> void forEach(AtomicReferenceArray<Y> map, @NotNull Consumer<? super Y> e) {
        for (int c = map.length(), j = -1; ++j < c; ) {
            Y v = map.getOpaque(j);
            if (v != null) {
                e.accept(v);
            }
        }
    }

    @Override
    public final boolean setCapacity(int _newCapacity) {

        int newCapacity = Math.max(_newCapacity, reprobes);

        if (capacity.getAndSet(newCapacity) != newCapacity) {


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

                List<V> removed = new FasterList<>();

                //copy items from the previous map into the new map. they will be briefly invisibile while they get transferred.  TODO verify
                forEachActive(this, prev[0], (b) -> {
                    if (put(b) == null)
                        removed.add(b);
                });

                commit(null);

                removed.forEach(this::onRemoved);
            }


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

        AtomicReferenceArray<V> newMap = new AtomicReferenceArray<>(capacity());

        AtomicReferenceArray<V> prevMap = map.getAndSet(newMap);

        commit(null);

        return prevMap;
    }

    @Nullable
    protected V update(Object x, @Nullable V adding /* null to remove */, float scale /* -1 to remove, 0 to get only*/) {

        boolean add = adding != null;
        boolean remove = (!add && (scale == -1));

        V added = null,
          found = null; //get: the found item,  remove: the hijacked item

        boolean merged = false;

        final int hash = hash(x);

        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;

        int iStart = i(c, hash);

        try {

            V target = null;
            for (int retry = 0; retry < reprobes; retry++ /*, dir = !dir*/) {

                int targetIndex = -1;

                float targetPri = Float.POSITIVE_INFINITY;
                int i = iStart; //dir ? iStart : (iStart + reprobes) - 1;

                for (int probe = 0; probe < reprobes; probe++) {

//                    if (i >= c) i -= c;
//                    else if (i < 0) i += c;

                    V current = map.get(i);

                    if (current == null) {
                        if (add && targetPri != Float.NEGATIVE_INFINITY) {
                            //empty, plan to take this if
                            // another empty has not been planned
                            // and only take it if the value has not been found elsewhere in the reprobe range, entirely scanned
                            target = null;
                            targetIndex = i;
                            targetPri = Float.NEGATIVE_INFINITY;
                        }
                    } else {
                        K y = key(current);
                        if (y == x || equals(y, x)) { //existing

                            if (!add) {

                                if (remove) { //remove
                                    if (map.compareAndSet(i, current, null)) {
                                        found = current;
                                    }
                                } else {
                                    found = current; //get
                                }

                            } else { //put
                                if (current != adding) {
                                    V next = merge(current, adding, scale);
                                    if (next!= current) {
                                        //replace
                                        if (!map.compareAndSet(i, current, next)) {
                                            //failed to replace; the original may have changed so continue and maybe reinsert in a new cell
                                        } else {
                                            //replaced
                                            targetIndex = -1;
                                            added = next;
                                            merged = true;
                                            break;
                                        }

                                    } else {
                                        //keep original
                                        targetIndex = -1;
                                        added = current;
                                        merged = true;
                                        break;
                                    }
                                }
                            }

                            break; //continue below

                        } else if (add) {
                            float iiPri = priSafe(current, -1);
                            if (targetPri > iiPri) {
                                //select a better target
                                target = current;
                                targetIndex = i;
                                targetPri = iiPri;
                            }
                            //continue probing; must try all
                        }
                    }

                    //i = dir ? (i + 1) : (i - 1);
                    i++; if (i == c) i = 0;

                }

                //add at target index
                if (targetIndex != -1) {

                    if (targetPri == Float.NEGATIVE_INFINITY) {

                        //insert to empty
                        if (map.compareAndSet(targetIndex, null, adding)) {
                            added = merge(null, adding, scale);
                        }

                    } else {
                        if (replace(adding, target)) {
                            if (map.compareAndSet(targetIndex, target, adding)) { //inserted
                                found = target;
                                added = merge(null, adding, scale);
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
                onRemoved(found);
            }

            if (added != null) {
                onAdded(added);
            } else if (add) {
                //rejected: add but not added and not merged
                pressurize(pri(adding) * scale);
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


        return add ? added : found;

    }

    protected int hash(Object x) {

        //return x.hashCode(); //default

        //identityComparisons ? System.identityHashCode(key)

        // "Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions."
        return Util.hashWangJenkins(x.hashCode());

    }


    /**
     * if no existing value then existing will be null.
     * this should modify the existing value if it exists,
     * or the incoming value if none.
     *
     * if adding content, pressurize() appropriately
     *
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
    @NotNull protected abstract V merge(@Nullable V existing, @NotNull V incoming, float scale);

    /**
     * can override in subclasses for custom replacement policy.
     * true allows the incoming to replace the existing.
     *
     * a potential eviction can be intercepted here
     */
    protected boolean replace(V incoming, V existing) {
        return replace(pri(incoming), pri(existing));
    }

    protected boolean replace(float incoming, float existing) {
        return hijackSoftmax(incoming, existing, random());
    }

    /**
     * can override in subclasses for custom equality test
     */
    protected boolean equals(K known, Object incoming) {
        return incoming.equals(known);
    }

    @Nullable
    @Override
    public V remove(@NotNull K k) {
        return update(k, null, -1);
    }

    protected boolean hijackSoftmax(float newPri, float oldPri, Random random) {

        float priEpsilon = priEpsilon();

        if (oldPri > priEpsilon) {
            return random.nextFloat() > (oldPri / ((newPri / reprobes) + oldPri));
        } else {
            return (newPri > priEpsilon) || random.nextFloat() > 0.5f / reprobes;
            // random.nextBoolean(); //50/50 chance
        }
    }

    /**
     */
    @Override
    public final V put(@NotNull V v, float scale, /* TODO */ @Nullable MutableFloat overflowing) {

        V y = update(key(v), v, scale);

        if (y != null)
            stretchRange(pri(y));

        return y;
    }


    /**
     * considers if this priority value stretches the current min/max range
     * this value will be updated for certain during a commit, so this value
     * only improves accuracy between commits.
     */
    private void stretchRange(float p) {
        if (p == p) {
            if (p > priMax) priMax = p;
            if (p < priMin) priMin = p;
        }
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
    public HijackBag<K, V> sample(int n, @NotNull IntObjectToIntFunction<? super V> target) {
        scan(n, target);
        return this;
    }

    @Nullable
    @Override
    public HijackBag<K, V> sample(int n, @NotNull Predicate<? super V> target) {
        scan(n, (h, v) -> {
            for (int i = 0; i < h; i++)
                target.test(v);
            return h;
        });
        return this;
    }

    @Nullable
    @Override
    public int pop(int n, @NotNull Predicate<? super V> target) {
        return scan(n, (h, v) -> {
            return (target.test(v) ? -1 : 0);
        });
    }


    public int scan(int n, IntObjectToIntFunction<? super V> each) {

        int s = size();
        if (s == 0)
            return 0;

        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return 0;


        final Random random = random();
        int i = random.nextInt(c);


//        float min = priMin;
//        float max = priMax;
//        float priRange = max - min;

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0


        //boolean di = random.nextBoolean(); //randomly choose traversal direction

        int selected = 0;

        //int nulls = c - s; //approximate number of empty slots that would be expected

        float priToHits = Math.max(1, ((n) / ((float)(s))));

        int skipped = 0;

        //final int N = n;
        int removed = 0;

        while (n > 0 && skipped < c) {

            //if (di) {
            if (++i == c) i = 0;
            /*} else {
                if (--i == -1) i = c - 1;
            }*/

            V v = map.getOpaque(i);
            if (v != null) {
                float p = pri(v);
                if (p == p) {

                    float fhits = (p * priToHits);
                    int hits = (int) Math.floor(fhits);
                    float remainder = fhits - hits;
                    if (remainder > 0) {
                        if (remainder >= random.nextFloat()) //use the change to select +1 probabalistically
                            hits++;
                    }

                    if (hits > 0) {
                        if (map.weakCompareAndSetVolatile(i, v, null)) {
                            int taken = each.intValueOf(hits, v);
                            boolean popped = (taken < 0);
                            if (popped) {
                                onRemoved(v);
                                removed++;
                                if (--s <= 0) {
                                    break;
                                } else {
                                    skipped++;
                                    taken = -taken; //make positive
                                }
                            } else if (taken > 0) {
                                //try to reinsert in that slot we removed it temporarily from
                                if (!map.compareAndSet(i, null, v)) {

                                    if (put(v) == null) { //try to insert as if normally
                                        //but if it didnt happen, give up, admit that it lost it
                                        onRemoved(v);
                                        removed++;
                                        if (--s <= 0)
                                            break;
                                        else {
                                            skipped++;
                                            continue;
                                        }
                                    }
                                }

                            }

                            n -= taken;
                            selected += taken;
                        }

                    } else {
                        skipped++;
                    }
                }
            } else {
                skipped++;
                //early deletion nullify
//                    if (map.compareAndSet(m, v, null)) {
//                        size.decrementAndGet();
//                        onRemoved(v);
//                    }
            }


        }

        if (removed > 0) {
            commit(null);
        }

        return selected;
    }


//    /**
//     * a value between 0 and 1 of how much percent of the priority range
//     * to accept an item which is below the current sampling target priority.
//     * generally this should be a monotonically increasing function of
//     * the scan progress proportion, a value in 0..1.0 also.
//     */
//    protected static float tolerance(float scanProgressProportion) {
//        return /*Util.sqr*/(Util.sqr(scanProgressProportion)); /* polynomial curve */
//    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void forEach(@NotNull Consumer<? super V> e) {
        forEachActive(this, e);
    }

//    /**
//     * yields the next threshold value to sample against
//     */
//    public float curve() {
//        float c = random.nextFloat();
//        return 1f - (c * c);
//    }

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

        double p = this.pressure.sumThenReset();
        int s = size();

        return commit(
            ((s > 0) && (p > 0)) ?
                Bag.forget(s, capacity(), (float) p, mass, temperature(), priEpsilon(), this::forget) :
                null
        );

    }

    abstract protected Consumer<V> forget(float rate);

    /**
     * higher value means faster forgetting
     */
    public float temperature() {
        return 0.5f;
    }

    protected float priEpsilon() {
        return Float.MIN_VALUE;
    }

    final AtomicBoolean busy = new AtomicBoolean(false);

    @NotNull
    @Override
    public HijackBag<K, V> commit(@Nullable Consumer<V> update) {

        if (!busy.compareAndSet(false, true))
            return this;

        try {
            if (update != null) {
                update(update);
            }

            float mass = 0;
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            int count = 0;

            AtomicReferenceArray<V> a = map.get();
            int len = a.length();
            for (int i = 0; i < len; i++) {
                V f = a.getOpaque(i);
                if (f == null)
                    continue;

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

            this.size = count;

            if (min == Float.POSITIVE_INFINITY) {
                this.priMin = 0;
                this.priMax = 0;
            } else {
                this.priMin = min;
                this.priMax = max;
            }

            this.mass = mass;

        } finally {
            busy.set(false);
        }

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
///**
// * ID of this bag, for use in constructing keys for the global treadmill
// */
// private final int id;

//    /**
//     * lock-free int -> int mapping used as a ticket barrier
//     */
//    static final class Treadmill {
//
//        static final ConcurrentLongSet map = new ConcurrentLongSet(Util.MAX_CONCURRENCY * 2);
//
//        static final AtomicInteger ticket = new AtomicInteger(0);
//
//        public static int newTarget() {
//            return ticket.incrementAndGet();
//        }
//
//        public static long start(int target, int hash) {
//
//            long ticket = (((long) target) << 32) | hash;
//
//            map.putIfAbsentRetry(ticket);
//
//            return ticket;
//        }
//
//        public static void end(long ticket) {
//            map.remove(ticket);
//        }
//    }
