package jcog.bag.impl;

import jcog.bag.Bag;
import jcog.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * the superclass's treadmill's extra data slots are used for storing:
 * 0=size
 * 1=capacity
 * this saves the space otherwise necessary for 2 additional AtomicInteger instances
 */
public abstract class HijackBag<K, V> extends Treadmill implements Bag<K, V> {

    /**
     * value index in the additional slots of the superclass Treadmill
     */
    final static int tSIZE = 0;
    final static int tCAPACITY = 1;

    public static final AtomicReferenceArray EMPTY_ARRAY = new AtomicReferenceArray(0);

    public final int reprobes;
    public transient final AtomicReference<AtomicReferenceArray<V>> map;

    public final DoubleAdder pressure = new DoubleAdder();

    float mass;

    public HijackBag(int initialCapacity, int reprobes) {
        this(reprobes);
        setCapacity(initialCapacity);
    }

    public HijackBag(int reprobes) {
        super(concurrency, 2 /* size, capacity */);
        this.reprobes = reprobes;
        this.map = new AtomicReference<>(EMPTY_ARRAY);
    }

    protected Random random() {
        return ThreadLocalRandom.current();
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
    public boolean setCapacity(int _newCapacity) {

        int newCapacity = Math.max(_newCapacity, reprobes);

        if (xGetAndSet(tCAPACITY, newCapacity) != newCapacity) {

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

                removed.forEach(this::_onRemoved);
            }


            return true;
        }

        return false;
    }


    @Override
    public void clear() {
        AtomicReferenceArray<V> x = reset();
        if (x != null) {
            forEachActive(this, x, this::_onRemoved);
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


        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;

        //int shuff = random().nextInt(reprobes);

        final int hash = x.hashCode(); /*hash(x)*/

        if (add || remove)
            start(hash);

        try {


            //for (int retry = 0; retry < reprobes; retry++ /*, dir = !dir*/)

            int iStart = Math.min(Math.abs(hash), Integer.MAX_VALUE - reprobes - 1); //dir ? iStart : (iStart + reprobes) - 1;
            int iEnd = (iStart + reprobes);
            int i = iStart;// + shuff;
            for (int probe = 0; probe < reprobes; probe++) {

                int ic = i % c;

                V current = map.get(ic);
                if (current != null) {
                    K y = key(current);
                    if (equals(y, x)) { //existing

                        if (add) { //put
                            if (current != adding) {
                                //float curPri = pri(current);
                                V next = merge(current, adding, scale);
                                if (next == null) {
                                    //merge failed, continue
                                } else if (next != current) {
                                    //replace
                                    if (!map.compareAndSet(ic, current, next)) {
                                        //failed to replace; the original may have changed so continue and maybe reinsert in a new cell
                                    } else {
                                        //replaced
                                        //pressurize(-curPri); //discount original priority
                                        added = next;
                                        merged = true;
                                        break;
                                    }

                                } else {
                                    //keep original
                                    added = current;
                                    merged = true;
                                    break;
                                }
                            }
                        } else {

                            if (remove) { //remove
                                if (map.compareAndSet(ic, current, null)) {
                                    found = current;
                                }
                            } else {
                                found = current; //get
                            }

                        }

                        break; //continue below

                    }
                }

                if (add && !merged) {

                    if (current == null || replace(adding, current, scale)) {
                        V toAdd = merge(current, adding, scale);
                        if (toAdd!=null && map.compareAndSet(ic, current, toAdd)) { //inserted
                            found = current;
                            added = toAdd;
                            break; //done
                        }
                    }
                }

                //i = dir ? (i + 1) : (i - 1);
                i++;
                if (i == iEnd) i = iStart;

            }

//                if (!add || (added != null))
//                    break;
            //else: try again

        } catch (Throwable t) {
            t.printStackTrace(); //should not happen
        } finally {
            if (add || remove) {
                end(hash);
            }
        }

        if (!merged) {
            if (found != null && (add || remove)) {
                _onRemoved(found);
            }

            if (added != null) {
                _onAdded(added);
            } else if (add) {
                //rejected: add but not added and not merged
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


//    protected int hash(Object x) {
//
//        //return x.hashCode(); //default
//
//        //identityComparisons ? System.identityHashCode(key)
//
//        // "Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions."
//        //return Util.hashWangJenkins(x.hashCode());
//
//        return x.hashCode();
//    }


    /**
     * if no existing value then existing will be null.
     * this should modify the existing value if it exists,
     * or the incoming value if none.
     * <p>
     * if adding content, pressurize() appropriately
     * <p>
     * <p>
     * NOTE:
     * this should usually equal the amount of priority increased by the
     * insertion (considering the scale's influence too) as if there were
     * no existing budget to merge with, even if there is.
     * <p>
     * this supports fairness so that existing items will not have a
     * second-order budgeting advantage of not contributing as much
     * to the presssure as new insertions.
     *
     * if returns null, the merge is considered failed and will try inserting/merging
     * at a different probe location
     */
    @Nullable
    protected abstract V merge(@Nullable V existing, @NotNull V incoming, float scale);

    /**
     * can override in subclasses for custom replacement policy.
     * true allows the incoming to replace the existing.
     * <p>
     * a potential eviction can be intercepted here
     */
    protected boolean replace(V incoming, V existing, float scale) {
        return replace(pri(incoming) * scale, pri(existing));
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

        pressurize(priSafe(v, 0) * scale);

        V y = update(key(v), v, scale);

        return y;
    }


    @Override
    public @Nullable V get(@NotNull Object key) {
        return update(key, null, 0);
    }

    @Override
    public int capacity() {
        return xGet(tCAPACITY);
    }

    @Override
    @Deprecated
    public void forEachWhile(@NotNull Predicate<? super V> each, int n) {
        throw new UnsupportedOperationException("yet");
    }


    @NotNull
    public HijackBag<K, V> sample(Bag.BagCursor<? super V> each) {

        int s = size();
        if (s <= 0)
            return this;

        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return this;

        final Random random = random();
        int i = random.nextInt(c);

        BagCursorAction next = BagCursorAction.Next;
        boolean modified = false;
        int nulls = 0; //emergency null counter
        while (!next.stop && nulls < c) {
            if (++i == c) i = 0; //modulo c
            V v = map.get(i);
            if (v != null) {
                if ((next = each.next(v)).remove) {
                    if (map.compareAndSet(i, v, null)) {
                        modified = true;
                        if (_onRemoved(v) <= 0)
                            break;
                    }
                }
                nulls = 0; //reset nulls
            } else {
                nulls++;
            }
        }

        if (modified)
            commit(null);

        return this;
//
////        float min = priMin;
////        float max = priMax;
////        float priRange = max - min;
//
//        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0
//
//
//        //boolean di = random.nextBoolean(); //randomly choose traversal direction
//
//        int selected = 0;
//
//        //int nulls = c - s; //approximate number of empty slots that would be expected
//
//        float priToHits = Math.max(1, ((n) / ((float)(s))));
//
//        int skipped = 0;
//
//        //final int N = n;
//        int removed = 0;
//
//        while (n > 0 && skipped < c) {
//
//            //if (di) {
//            if (++i == c) i = 0;
//            /*} else {
//                if (--i == -1) i = c - 1;
//            }*/
//
//            V v = map.getOpaque(i);
//            if (v != null) {
//                float p = pri(v);
//                if (p == p) {
//
//                    float fhits = (p * priToHits);
//                    int hits = (int) Math.floor(fhits);
//                    float remainder = fhits - hits;
//                    if (remainder > 0) {
//                        if (remainder >= random.nextFloat()) //use the change to select +1 probabalistically
//                            hits++;
//                    }
//
//                    if (hits > 0) {
//                        if (map.weakCompareAndSetVolatile(i, v, null)) {
//                            int taken = each.intValueOf(hits, v);
//                            boolean popped = (taken < 0);
//                            if (popped) {
//                                onRemoved(v);
//                                removed++;
//                                if (--s <= 0) {
//                                    break;
//                                } else {
//                                    skipped++;
//                                    taken = -taken; //make positive
//                                }
//                            } else if (taken > 0) {
//                                //try to reinsert in that slot we removed it temporarily from
//                                if (!map.compareAndSet(i, null, v)) {
//
//                                    if (put(v) == null) { //try to insert as if normally
//                                        //but if it didnt happen, give up, admit that it lost it
//                                        onRemoved(v);
//                                        removed++;
//                                        if (--s <= 0)
//                                            break;
//                                        else {
//                                            skipped++;
//                                            continue;
//                                        }
//                                    }
//                                }
//
//                            }
//
//                            n -= taken;
//                            selected += taken;
//                        }
//
//                    } else {
//                        skipped++;
//                    }
//                }
//            } else {
//                skipped++;
//                //early deletion nullify
////                    if (map.compareAndSet(m, v, null)) {
////                        size.decrementAndGet();
////                        onRemoved(v);
////                    }
//            }
//
//
//        }
//
//        if (removed > 0) {
//            commit(null);
//        }
//
//        return selected;
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
        return xGet(tSIZE);
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

//    @Override
//    public float priMin() {
//        throw new UnsupportedOperationException();
//    }
//    @Override
//    public float priMax() {
//        throw new UnsupportedOperationException();
//    }

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

    //final AtomicBoolean busy = new AtomicBoolean(false);

    @NotNull
    @Override
    public HijackBag<K, V> commit(@Nullable Consumer<V> update) {

//        if (!busy.compareAndSet(false, true))
//            return this;

        try {
            if (update != null) {
                update(update);
            }

            float mass = 0;

            int count = 0;

            AtomicReferenceArray<V> a = map.get();
            int len = a.length();
            for (int i = 0; i < len; i++) {
                V f = a.get(i);
                if (f == null)
                    continue;

                float p = pri(f);
                if (p == p) {
                    mass += p;
                    count++;
                } else {
                    if (a.compareAndSet(i, f, null)) {
                        _onRemoved(f); //TODO this may call onRemoved unnecessarily if the map has changed (ex: resize)
                    }
                }
            }

            //assert(size() == count);
            xSet(tSIZE, count);

            this.mass = mass;

        } finally {
            //   busy.set(false);
        }

        return this;
    }

    private void _onAdded(V x) {
        xIncrementAndGet(tSIZE);
        onAdded(x);
    }

    private int _onRemoved(V x) {
        int s = xDecrementAndGet(tSIZE);
        onRemoved(x);
        return s;
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

//    /**
//     * ID of this bag, for use in constructing keys for the global treadmill
//     */
//     private final int id = TreadmillMutex.newTarget();
//
//    /**
//     * lock-free int -> int mapping used as a ticket barrier
//     */
//    static final class TreadmillMutex {
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
