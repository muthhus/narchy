package jcog.bag.impl;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.bag.Bag;
import jcog.bag.util.Treadmill;
import jcog.list.FasterList;
import jcog.pri.Prioritized;
import jcog.pri.op.PriForget;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.bag.impl.HijackBag.Mode.*;

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

    public final AtomicDouble pressure = new AtomicDouble();

    public float mass;
    private float min;
    private float max;

    private static final float PRESSURE_THRESHOLD = 0.05f;

    protected HijackBag(int initialCapacity, int reprobes) {
        this(reprobes);
        setCapacity(initialCapacity);
    }

    protected HijackBag(int reprobes) {
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
        pressure.addAndGet(f);
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
    public void setCapacity(int _newCapacity) {

        int newCapacity = Math.max(_newCapacity, reprobes);

        if (xGetAndSet(tCAPACITY, newCapacity) != newCapacity) {

            final AtomicReferenceArray<V>[] prev = new AtomicReferenceArray[1];

            //ensures sure only the thread successful in changing the map instance is the one responsible for repopulating it,
            //in the case of 2 simultaneous threads deciding to allocate a replacement:
            AtomicReferenceArray<V> next = newCapacity != 0 ? new AtomicReferenceArray<>(newCapacity) : EMPTY_ARRAY;
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


            //return true;
        }

        //return false;
    }


    @Override
    public void clear() {
        AtomicReferenceArray<V> x = reset();
        if (x != null) {
            forEachActive(this, x, this::_onRemoved);
        }

    }

    @Nullable
    private AtomicReferenceArray<V> reset() {

        if (!xCompareAndSet(tSIZE, 0, 0)) {
            AtomicReferenceArray<V> newMap = new AtomicReferenceArray<>(capacity());

            AtomicReferenceArray<V> prevMap = map.getAndSet(newMap);

            commit();

            return prevMap;
        }

        return null;
    }

    enum Mode {
        GET, PUT, REMOVE
    }

    private V update(@NotNull Object k, @Nullable V incoming /* null to remove */, Mode mode, @Nullable MutableFloat overflowing) {

        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;



        final int hash = k.hashCode(); /*hash(x)*/

        float incomingPri;
        if (mode == Mode.PUT) {
            incomingPri = pri(incoming);
            if (incomingPri != incomingPri)
                return null;
        } else {
            incomingPri = Float.POSITIVE_INFINITY; /* shouldnt be used */
        }

        V toAdd = null, toRemove = null, toReturn = null;
        try {

            int start = (hash % c); //Math.min(Math.abs(hash), Integer.MAX_VALUE - reprobes - 1); //dir ? iStart : (iStart + reprobes) - 1;
            if (start < 0)
                start += c; //Fair wraparound: ex, -1 -> (c-1)

            if (mode != GET)
                start(hash);

            probing:
            for (int i = start, probe = reprobes; probe > 0; probe--) {

                V p = map.get(i); //probed value 'p'

                if (p != null && k.equals(key(p))) { //existing, should only occurr at most ONCE in this loop
                    switch (mode) {

                        case GET:
                            toReturn = p;
                            break;

                        case PUT:
                            if (p == incoming) {
                                toReturn = p; //identical match found, keep original
                            } else {
                                V next = merge(p, incoming, overflowing);
                                if (next != null && (next == p || map.compareAndSet(i, p, next))) {
                                    if (next != p) {
                                        toRemove = p; //replaced
                                        toAdd = next;
                                    }
                                    toReturn = next;
                                }
                            }
                            break;

                        case REMOVE:
                            if (map.compareAndSet(i, p, null)) {
                                toReturn = toRemove = p;
                            }
                            break;
                    }

                    break probing; //successful if y!=null
                }

                if (++i == c) i = 0; //continue to next probed location
            }

            if (mode == PUT && toReturn == null) {
                //attempt insert
                inserting:
                for (int i = start, probe = reprobes; probe > 0; probe--) {

                    V existing = map.compareAndExchange(i, null, incoming);//probed value 'p'

                    if (existing == null) {

                        toReturn = toAdd = incoming;
                        break inserting; //took empty slot, done

                    } else {
                        //attempt HIJACK (tm)
                        if (replace(incomingPri, existing)) {
                            if (map.compareAndSet(i, existing, incoming)) { //inserted
                                toRemove = existing;
                                toReturn = toAdd = incoming;
                                break inserting; //hijacked replaceable slot, done
                            }
                        }

                    }

                    if (++i == c) i = 0; //continue to next probed location
                }

            }

        } catch (Throwable t) {
            t.printStackTrace(); //should not happen
        } finally {
            if (mode != GET)
                end(hash);
        }

        if (toRemove != null) {
            _onRemoved(toRemove);
        }
        if (toAdd != null) {
            _onAdded(toAdd);
        }

        return toReturn;
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
     * <p>
     * if returns null, the merge is considered failed and will try inserting/merging
     * at a different probe location
     */
    protected abstract V merge(@NotNull V existing, @NotNull V incoming, @Nullable MutableFloat overflowing);

    /**
     * can override in subclasses for custom replacement policy.
     * true allows the incoming to replace the existing.
     * <p>
     * a potential eviction can be intercepted here
     */
    protected boolean replace(float i, V existing) {
        float e = pri(existing);
        if (e != e)
            return true;
        return replace(i, e);
    }

    protected boolean replace(float incoming, float existing) {
        return hijackSoftmax(incoming, existing);
    }

    @Nullable
    @Override
    public V remove(@NotNull K k) {
        return update(k, null, REMOVE, null);
    }

    protected boolean hijackSoftmax(float newPri, float oldPri) {
        return hijackSoftmax(newPri, oldPri, 1f);
    }

    protected boolean hijackSoftmax(float newPri, float oldPri, float temperature) {


        float priEpsilon = Prioritized.EPSILON;

        if (oldPri > priEpsilon) {
            assert (temperature < reprobes);

            float newPriSlice = newPri / (reprobes / temperature);
            float thresh = newPriSlice / (newPriSlice + oldPri);
            return random().nextFloat() < thresh;
        } else {
            return (newPri >= priEpsilon) || random().nextBoolean();// / reprobes;
            // random.nextBoolean(); //50/50 chance
        }
    }


    protected boolean hijackSoftmax2(float newPri, float oldPri, Random random) {
        //newPri = (float) Math.exp(newPri*2*reprobes); //divided by temperature, reprobes ~ 0.5/temperature
        //oldPri = (float) Math.exp(oldPri*2*reprobes);
        newPri = newPri * newPri * reprobes;
        oldPri = oldPri * oldPri * reprobes;
        if (oldPri > 2 * Float.MIN_VALUE) {
            float thresh = 1f - (1f - (oldPri / (newPri + oldPri)));
            return random.nextFloat() > thresh;
        } else {
            return (newPri >= Float.MIN_VALUE) || random.nextBoolean();// / reprobes;
            // random.nextBoolean(); //50/50 chance
        }
    }

    /**
     */
    @Override
    public final V put(@NotNull V v,  /* TODO */ @Nullable MutableFloat overflowing) {

//        float p = pri(v);
//        if (p != p)
//            return null; //already deleted
        K k = key(v);
        if (k == null)
            return null;

        int c = capacity();
        int s = size();
        if (((float)Math.abs(c - s)) / c < PRESSURE_THRESHOLD)
            pressurize(pri(v));

        V x = update(k, v, PUT, overflowing);
        if (x == null)
            onReject(v);
        return x;
    }


    @Override
    public @Nullable V get(@NotNull Object key) {
        return update(key, null, GET, null);
    }

    @Override
    public int capacity() {
        return xGet(tCAPACITY);
    }

    @Override
    @NotNull
    public HijackBag<K, V> sample(@NotNull Bag.BagCursor<? super V> each) {
        final int s = size();
        if (s <= 0)
            return this;

        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return this;

        final Random random = random();
        int i = random.nextInt(c);

        BagSample next = BagSample.Next;
        //boolean modified = false;
        int seen = 0;
        while (seen++ < c) {
            V v = map.get(i);
            float p;
            if (v != null && ((p= pri(v))==p /* not deleted*/)) {
                next = each.next(v);
                if (next.remove) {
                    if (map.compareAndSet(i, v,null)) {
                        //modified = true;
                        //else: already removed

                        if (_onRemoved(v) <= 0)
                            break;
                    }
                }
            }
            if (next.stop)
                break;
            if (++i == c) i = 0; //modulo c
        }

        /*if (modified)
            commit(null);*/

        return this;
    }


    @Override
    public int size() {
        return Math.max(0, xGet(tSIZE));
    }

    @Override
    public void forEach(@NotNull Consumer<? super V> e) {
        forEachActive(this, e);
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

    @Override
    public Stream<V> stream() {
        AtomicReferenceArray<V> map = this.map.get();
        return IntStream.range(0, map.length()).mapToObj(map::get).filter(Objects::nonNull);
    }


    /**
     * always >= 0
     */
    @Override
    public float depressurize() {
        float pv = (float) pressure.getAndSet(0);
        if (pv >= 0) {
            return pv;
        } else {
            pressure.set(0);
            return 0;
        }
    }

    @Override
    @Deprecated
    public Bag<K, V> commit() {

        float p = depressurize();
        int s = size();

        return commit(
                ((s > 0) && (p > 0)) ?
                        PriForget.forget(s, capacity(), p, mass, PriForget.DEFAULT_TEMP, Prioritized.EPSILON, this::forget) :
                        null
        );

    }

    @Override
    public float priMin() {
        return min;
    }

    @Override
    public float priMax() {
        return max;
    }

    abstract protected Consumer<V> forget(float rate);


    //final AtomicBoolean busy = new AtomicBoolean(false);

    @NotNull
    @Override
    public HijackBag<K, V> commit(@Nullable Consumer<V> update) {


//        try {
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
                V f = a.get(i);
                if (f == null)
                    continue;

                float p = pri(f);
                if (p == p) {
                    mass += p;
                    if (p > max) max = p;
                    if (p < min) min = p;
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
            if (count > 0) {
                this.min = min;
                this.max = max;
            } else {
                this.min = this.max = 0;
            }

//        } finally {
//            //   busy.set(false);
//        }

        return this;
    }

    private void _onAdded(V x) {
        xIncrementAndGet(tSIZE);
        onAdd(x);
    }

    private int _onRemoved(V x) {
        int s = xDecrementAndGet(tSIZE);
        onRemove(x);
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
