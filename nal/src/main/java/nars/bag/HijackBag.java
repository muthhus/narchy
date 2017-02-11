package nars.bag;

import jcog.Util;
import nars.$;
import nars.Param;
import nars.budget.Budget;
import nars.budget.BudgetMerge;
import nars.link.BLink;
import nars.link.RawBLink;
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
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
public class HijackBag<X> implements Bag<X,BLink<X>> {

    public static final AtomicReferenceArray EMPTY_ARRAY = new AtomicReferenceArray(0);
    private final Random random;
    private final int reprobes;

    transient final AtomicReference<AtomicReferenceArray<BLink<X>>> map;

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

    final AtomicInteger size = new AtomicInteger(0), capacity = new AtomicInteger(0);

    /**
     * hash -> ticket
     */
    final ConcurrentHashMapUnsafe<Integer, Integer> busy = new ConcurrentHashMapUnsafe<>();

    final AtomicInteger ticket = new AtomicInteger(0);

    public HijackBag(int capacity, int reprobes, BudgetMerge merge, Random random) {
        this(reprobes, merge, random);
        setCapacity(capacity);
    }

    public HijackBag(int reprobes, BudgetMerge merge, Random random) {
        this.merge = merge;
        this.reprobes = reprobes;
        this.random = random;
        this.map = new AtomicReference<>();
        this.map.set(EMPTY_ARRAY);
    }

    public boolean setCapacity(int newCapacity) {


        if (capacity.getAndSet(newCapacity)!=newCapacity) {

            List<BLink> removed = $.newArrayList();

            final AtomicReferenceArray<BLink<X>>[] prev = new AtomicReferenceArray[1];

            //ensures sure only the thread successful in changing the map instance is the one responsible for repopulating it,
            //in the case of 2 simultaneous threads deciding to allocate a replacement:
            AtomicReferenceArray<BLink<X>> next = newCapacity != 0 ? new AtomicReferenceArray<BLink<X>>(newCapacity) : EMPTY_ARRAY;
            if (next == this.map.updateAndGet((x) -> {
                if (x.length() != newCapacity) {
                    prev[0] = x;
                    return next;
                }
                else return x;
            })) {
                //copy items from the previous map into the new map. they will be briefly invisibile while they get transferred.  TODO verify
                forEach(prev[0], false, (b) -> {
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
            forEach(x -> {
                x.delete();
                onRemoved(x);
            });
            //Arrays.fill(map, null);
        }
    }

    @Nullable
    protected BLink<X> update(Object x, @Nullable BLink<X> adding /* null to remove */, float scale /* -1 to remove, 0 to get only*/) {

        AtomicReferenceArray<BLink<X>> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;

        int hash = x.hashCode();

        boolean add = adding != null;
        boolean remove = (!add && (scale == -1));

        int targetIndex = -1;
        float targetPri = Float.POSITIVE_INFINITY;

        BLink<X> added = null;
        float addingPri = adding != null ? adding.priSafe(0) : Float.NaN;

        BLink<X> target = null;
        BLink<X> found = null; //get or remove

        boolean merged = false;
        final int ticket = add ? busy(hash) : Integer.MIN_VALUE /* N/A for get or remove */;


        int i = i(c, hash);

        try {
            for (int r = 0; r < reprobes; r++) {
                i++;
                if (i == c) i = 0; //wrap-around

                BLink<X> ii = map.get(i);

                if (ii == null) {
                    if (add) {
                        //empty, insert if not found elsewhere in the reprobe range
                        target = ii;
                        targetIndex = i;
                        targetPri = Float.NEGATIVE_INFINITY;
                    }
                } else {
                    X iiv = ii.get();
                    boolean sameInstance;
                    if ((sameInstance = (iiv == x)) || equals(x, iiv)) { //existing

                        if (!add) {

                            if (remove) { //remove
                                if (map.compareAndSet(i, ii, null)) {
                                    found = ii;
                                }
                            } else {
                                found = ii; //get
                            }

                        } else { //put
                            float pBefore = ii.priSafe(0);
                            merge.apply(ii, adding, scale); //TODO overflow
                            pressure += ii.priSafe(0) - pBefore;

                            targetIndex = -1;
                            added = ii;
                            merged = true;
                        }

                        break; //continue below

                    } else if (add) {
                        float iiPri = ii.priSafe(-1);
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
                if (targetPri == Float.NEGATIVE_INFINITY) {

                    BLink<X> adding2 = adding.cloneScaled(merge, scale);

                    //insert to empty
                    if (map.compareAndSet(targetIndex, null, adding2)) {
                        added = adding2;
                        pressure += adding2.priSafe(0);
                    }

                } else {
                    if (replace(addingPri, targetPri)) {
                        if (map.compareAndSet(targetIndex, target, adding)) {
                            //inserted
                            pressure += Math.max(0, addingPri - targetPri);

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
                found.delete(); //ensure deleted
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


    /*private static int i(int c, int hash, int r) {
        return (int) ((Integer.toUnsignedLong(hash) + r) % c);
    }*/

    private static int i(int c, int hash) {
        return (int) (Integer.toUnsignedLong(hash) % c);
    }

    /**
     * can override in subclasses for custom replacement policy
     */
    protected boolean replace(float incomingPri, float existingPri) {
        return hijackSoftmax(incomingPri, existingPri);
    }

    /**
     * can override in subclasses for custom equality test
     */
    protected boolean equals(Object x, X y) {
        return x.equals(y);
    }

    @Nullable
    @Override
    public BLink<X> remove(X x) {
        return update(x, null, -1);
    }


    private boolean hijackSoftmax(float newPri, float oldPri) {

        boolean oldPriThresh = oldPri > Param.BUDGET_EPSILON;
        if (!oldPriThresh) {
            boolean newPriThresh = newPri > Param.BUDGET_EPSILON;
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
    public BLink<X> put(@NotNull X x, @NotNull BLink<X> bb, float scale, /* TODO */ @Nullable MutableFloat overflowing) {

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

        BLink<X> y = update(x, bb, scale);

        if (y != null)
            range(y.priSafe(0));

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
            throw new Budget.BudgetException();
            //return p;
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
    public @Nullable BLink<X> get(@NotNull Object key) {
        return update(key, null, 0);
    }

    @Override
    public int capacity() {
        return capacity.get();
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @Nullable
    @Override
    public HijackBag<X> sample(int n, @NotNull Predicate<? super BLink<X>> target) {
        AtomicReferenceArray<BLink<X>> map = this.map.get();
        int c = map.length();
        if (c == 0)
            return null;

        /*if (isEmpty())
            return null;*/

        int jLimit = (int) Math.ceil(c * SCAN_ITERATIONS);

        int i = random.nextInt(c), j = 0;

        int batchSize = n;

        float r = curve(); //randomized threshold

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        boolean di = random.nextBoolean();

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {

            BLink<X> v = map.get(i);
            if (di) {
                if (++i == c) i = 0;
            } else {
                if (--i == -1) i = c-1;
            }


            if (v != null) {

                float p = v.pri();
                if (p == p) {

                    if ((r < p) || (r < p + tolerance(j, jLimit, n, batchSize, c))) {
                        if (target.test(v)) {
                            n--;
                            r = curve();
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
        }

        return this;
    }


    @Override
    public int size() {
        return size.get();
    }

    @Override
    public void forEach(@NotNull Consumer<? super BLink<X>> e) {
        if (isEmpty())
            return;

        forEach(this.map.get(), false, e);
    }

    public static <X> void forEach(AtomicReferenceArray<BLink<X>> map, boolean acceptDeleted, @NotNull Consumer<? super BLink<X>> e) {
        int c = map.length();
        if (c == 0)
            return;

        int j = -1;
        while (++j < c) {
            BLink<X> v = map.get(j);

            if (v != null) {
                float p = v.pri();
                if (acceptDeleted || (p == p) /* NaN? */) {
                    e.accept(v);
                } else {
                    /*if (map.compareAndSet(m, v, null))
                        size.decrementAndGet();*/
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
        return /*Util.sqr*/(Util.sqr(searchProgress * searchProgress));// * searchProgress);

        /*
        float exp = 6;
        return float) Math.pow(searchProgress, exp);// + selectionRate;*/
    }


    @Override
    public Spliterator<BLink<X>> spliterator() {
        return stream().spliterator();
    }

    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
        return stream().iterator();
    }

    public Stream<BLink<X>> stream() {
        AtomicReferenceArray<BLink<X>> map = this.map.get();
        return IntStream.range(0, map.length()).mapToObj(map::get).filter(Objects::nonNull);
    }

    @Override
    public boolean contains(@NotNull X it) {
        return get(it) != null;
    }

    @NotNull
    @Override
    public HijackBag<X> commit(Function<Bag, Consumer<BLink>> update) {


        final float[] mass = {0};

        final float[] min = {Float.POSITIVE_INFINITY};
        final float[] max = {Float.NEGATIVE_INFINITY};
        final int[] count = {0};

        forEach((BLink<X> f) -> {
            float p = f.priSafe(0);
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
    public HijackBag<X> update(@Nullable Consumer<BLink> each) {

        if (each != null) {
            this.pressure = 0;
            forEach(each);
        }

        return this;
    }


    @Override
    public BLink<X> mul(@NotNull Object key, float factor) {
        BLink<X> b = get(key);
        if (b != null && !b.isDeleted()) {
            float before = b.priSafe(0);
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
            float before = b.priSafe(0);
            b.priAdd(x);
            float after = range(b.pri());
            pressure += (after - before);
            return b;
        }
        return null;
    }


}
