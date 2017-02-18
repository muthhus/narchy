package jcog.bag.impl;

import jcog.bag.Bag;

import jcog.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by me on 2/17/17.
 */
public abstract class HijackBag<K,V> implements Bag<K,V> {

    public static final AtomicReferenceArray EMPTY_ARRAY = new AtomicReferenceArray(0);

    /**
     * max # of times allowed to scan through until either the next item is
     * accepted with final tolerance or gives up.
     * for safety, should be >= 1.0
     */
    private static final float SCAN_ITERATIONS = 1f;
    protected final Random random;
    protected final int reprobes;
    protected transient final AtomicReference<AtomicReferenceArray<V>> map;
    final AtomicInteger size = new AtomicInteger(0);
    final AtomicInteger capacity = new AtomicInteger(0);
    /**
     * hash -> ticket
     */
    final ConcurrentHashMapUnsafe<Integer, Integer> busy = new ConcurrentHashMapUnsafe<>();
    final AtomicInteger ticket = new AtomicInteger(0);
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
        this.map = new AtomicReference<>(EMPTY_ARRAY);
    }

    private static int i(int c, int hash) {
        return (int) (Integer.toUnsignedLong(hash) % c);
    }

    private static boolean hijackGreedy(float newPri, float weakestPri) {
        return weakestPri <= newPri;
    }

    public static <X,Y> void forEachActive(@NotNull HijackBag<X,Y> bag, @NotNull Consumer<? super Y> e) {
        forEachActive(bag, bag.map.get(), e);
    }

    public static <X,Y> void forEachActive(@NotNull HijackBag<X,Y> bag, @NotNull AtomicReferenceArray<Y> map, @NotNull Consumer<? super Y> e) {
        forEach(map, bag::active, e);
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
    public boolean setCapacity(int newCapacity) {


        if (capacity.getAndSet(newCapacity)!=newCapacity) {

            List<V> removed = new FasterList();

            final AtomicReferenceArray<V>[] prev = new AtomicReferenceArray[1];

            //ensures sure only the thread successful in changing the map instance is the one responsible for repopulating it,
            //in the case of 2 simultaneous threads deciding to allocate a replacement:
            AtomicReferenceArray<V> next = newCapacity != 0 ? new AtomicReferenceArray<V>(newCapacity) : EMPTY_ARRAY;
            if (next == this.map.updateAndGet((x) -> {
                if (x.length() != newCapacity) {
                    prev[0] = x;
                    return next;
                }
                else return x;
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
        if (!size.compareAndSet(0, 0)) {
            AtomicReferenceArray<V> prevMap = map.getAndSet(new AtomicReferenceArray<V>(capacity()));
            forEachActive(this, prevMap, this::onRemoved);
            //Arrays.fill(map, null);
        }
    }

    @Nullable
    protected V update(Object x, @Nullable V adding /* null to remove */, float scale /* -1 to remove, 0 to get only*/) {

        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;

        int hash = x.hashCode();

        boolean add = adding != null;
        boolean remove = (!add && (scale == -1));

        int targetIndex = -1;
        float targetPri = Float.POSITIVE_INFINITY;

        V added = null;

        V target = null;
        V found = null; //get or remove

        boolean merged = false;
        final int ticket = add ? busy(hash) : Integer.MIN_VALUE /* N/A for get or remove */;


        int i = i(c, hash);

        try {
            for (int r = 0; r < reprobes; r++) {
                i++;
                if (i == c) i = 0; //wrap-around

                V ii = map.get(i);

                if (ii == null) {
                    if (add) {
                        //empty, insert if not found elsewhere in the reprobe range
                        target = ii;
                        targetIndex = i;
                        targetPri = Float.NEGATIVE_INFINITY;
                    }
                } else {
                    K iiv = key(ii);
                    if (equals(x, iiv)) { //existing

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
                        if (targetPri >= iiPri) {
                            //select a better target
                            target = ii;
                            targetIndex = i;
                            targetPri = iiPri;
                        }
//
                        //continue probing; must try all
                    }
                }
            }

            //add at target index
            if (targetIndex != -1) {

                pressure += merge(null, adding, scale);

                if (targetPri == Float.NEGATIVE_INFINITY) {

                    //insert to empty
                    if (map.compareAndSet(targetIndex, null, adding)) {
                        added = adding;
                    }

                } else {
                    if (replace(pri(adding), targetPri)) {
                        if (map.compareAndSet(targetIndex, target, adding)) { //inserted
                            //pressure -= targetPri;
                            found = target;
                            added = adding;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace(); //should not happen
        }

        if (add) {
            unbusy(hash/*, ticket*/);
        }

        if (!merged) {
            if ((add || remove) && found != null) {
                size.decrementAndGet();
                onRemoved(found);
            }

            if (added != null) {
                size.incrementAndGet();
                onAdded(added);
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
     * @return the pressure increase the merge has caused.
     * */
    protected abstract float merge(@Nullable V existing, @NotNull V incoming, float scale);

    /**
     * can override in subclasses for custom replacement policy
     */
    protected boolean replace(float incomingPri, float existingPri) {
        return hijackSoftmax(incomingPri, existingPri);
    }

    /**
     * can override in subclasses for custom equality test
     */
    protected boolean equals(Object x, K y) {
        return x.equals(y);
    }

    @Nullable
    @Override
    public V remove(K k) {
        return update(k, null, -1);
    }

    private boolean hijackSoftmax(float newPri, float oldPri) {

        float priEpsilon = priEpsilon();

        boolean oldPriThresh = oldPri > priEpsilon;
        if (!oldPriThresh) {
            boolean newPriThresh = newPri > priEpsilon;
            if (newPriThresh)
                return true;
            else
                return random.nextBoolean(); //50/50 chance
        } else {
            return (random.nextFloat() > (oldPri / (newPri + oldPri)));
        }
    }

    @Override
    public V put(@NotNull V bb, float scale, /* TODO */ @Nullable MutableFloat overflowing) {

//        BLink<X> bb;
//        if (b instanceof BLink) {
//            bb = (BLink) b;
//        } else {
//            float p = b.pri();
//            if (p!=p)
//                return null; //deleted
//            float q = b.qua();
//            bb = ArrayBag.newLink(x, null);
//            bb.setBudget(p, q);
//        }

        V y = update(key(bb), bb, scale);

        if (y != null)
            range(priSafe(y, 0));

        return y;
    }

    public int busy(int hash) {
        int ticket = this.ticket.incrementAndGet();

        while (busy.putIfAbsent(hash, ticket) != null) {
            //System.out.println("wait");
        }

        return ticket;
    }

    public void unbusy(int x/*, int ticket*/) {
        /*boolean freed = */
        Integer freed = busy.remove(x/*, ticket*/);
        /*if (freed==null || freed!=ticket)
            throw new RuntimeException("insertion fault");*/
    }

    /**
     * considers if this priority value stretches the current min/max range
     * this value will be updated for certain during a commit, so this value
     * only improves accuracy between commits.
     */
    private float range(float p) {
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
    public HijackBag<K,V> sample(int n, @NotNull Predicate<? super V> target) {
        AtomicReferenceArray<V> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;

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

        float tolerance = 0, toleranceInc = priRange / jLimit;

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {

            V v = map.get(i);
            if (di) {
                if (++i == c) i = 0;
            } else {
                if (--i == -1) i = c-1;
            }


            if (v != null) {

                float p = pri(v);
                if (p == p) {

                    float r = curve() * priRange + priMin; //randomized threshold

                    if (r < p + tolerance(((float)j)/jLimit)) { /*|| (r < p + tolerance(j, jLimit, n, batchSize, c))*/
                        if (target.test(v)) {
                            n--;
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

            j++;
            tolerance += toleranceInc;
        }

        return this;
    }


    /**
     * a value between 0 and 1 of how much percent of the priority range
     * to accept an item which is below the current sampling target priority.
     * generally this should be a monotonically increasing function of
     * the scan progress proportion, a value in 0..1.0 also.
     * */
    protected float tolerance(float scanProgressProportion) {
        return scanProgressProportion * scanProgressProportion /* power 2 curve */;
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
    public boolean contains(K it) {
        return get(it) != null;
    }

    @Override
    @Deprecated public Bag<K,V> commit() {
        return commit((b)-> Bag.forget(
                b.size(), pressure, mass, temperature(), priEpsilon(), this::forget));
    }

    protected float temperature() {
        return 0.5f;
    }
    protected float priEpsilon() {
        return Float.MIN_VALUE;
    }

    protected abstract Consumer<V> forget(float rate);

    @NotNull
    @Override
    public HijackBag<K,V> commit(Function<Bag<K,V>, Consumer<V>> update) {


        final float[] mass = {0};

        final float[] min = {Float.POSITIVE_INFINITY};
        final float[] max = {Float.NEGATIVE_INFINITY};
        final int[] count = {0};

        forEach((V f) -> {
            float p = priSafe(f, 0);
            count[0]++;
            if (p > max[0]) max[0] = p;
            if (p < min[0]) min[0] = p;
            mass[0] += p;
        });

        this.size.set(count[0]);


        float MIN = min[0];
        if (MIN == Float.POSITIVE_INFINITY) {
            this.priMin = 0;
            this.priMax = 0;
        } else {
            this.priMin = MIN;
            this.priMax = max[0];
        }

        this.mass = mass[0];

//        Forget f;
//        if (existingMass > 0 && pressure > 0) {
//            float p = this.pressure;
//            f = Forget.forget(p, existingMass, count[0], Param.BAG_THRESHOLD);
//        } else {
//            f = null;
//        }


        update(update != null ? update.apply(this) : null);

        return this;
    }

    @NotNull
    public HijackBag<K,V> update(@Nullable Consumer<V> each) {

        if (each != null) {
            this.pressure = 0;
            forEach(each);
        }

        return this;
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
