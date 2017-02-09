package nars.bag.experimental;

import nars.Param;
import nars.bag.Bag;
import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.link.BLink;
import nars.link.DefaultBLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * unsorted priority queue with stochastic replacement policy
 *
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
public class HijackBag<X> implements Bag<X> {

    private final Random random;
    private final int reprobes;

    transient AtomicReferenceArray<BLink<X>> map;

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

    /** hash -> ticket */
    final ConcurrentHashMapUnsafe<Integer, Integer> busy = new ConcurrentHashMapUnsafe<>();

    final AtomicInteger ticket = new AtomicInteger(0);

    public HijackBag(int capacity, int reprobes, BudgetMerge merge, Random random) {
        this.merge = merge;
        this.map = new AtomicReferenceArray<BLink<X>>(capacity);
        this.reprobes = reprobes;
        this.random = random;

    }


    @Override
    public void clear() {
        if (!count.compareAndSet(0, 0)) {
            forEach(x -> {
                x.delete();
                onRemoved(x);
            });
            //Arrays.fill(map, null);
        }
    }

    @Nullable
    protected BLink<X> update(Object x, @Nullable BLink<X> adding /* null to remove */, float scale /* -1 to remove, 0 to get only*/) {
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

        int c = capacity();

        try {
            for (int r = 0; r < reprobes; r++) {
                int i = (hash + r) & (c - 1);

                BLink<X> ii = map.get(i);

                if (ii == null) {
                    if (add) {
                        //empty, insert if not found elsewhere in the reprobe range
                        target = ii;
                        targetIndex = i;
                        targetPri = Float.NEGATIVE_INFINITY;
                    }
                } else {
                    if (equals(x, ii.get())) { //existing

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
                            added = ii;
                            targetIndex = -1;
                            merged = true;
                        }

                        break; //continue below

                    } else if (add) {
                        float iiPri = ii.priSafe(-1);
                        if (targetPri >= iiPri) {
                            //select a better garget
                            target = ii;
                            targetIndex = i;
                            targetPri = iiPri;
                        }
                        //continue probing; must try all
                    }
                }
            }

            //add at target index
            if (targetIndex != -1) {
                if (targetPri == Float.NEGATIVE_INFINITY) {

                    DefaultBLink adding2 = new DefaultBLink(x);
                    adding2.setBudget(0, adding.qua()); //use the incoming quality.  budget will be merged
                    merge.apply(adding2, adding, scale);

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
            unbusy(hash, ticket);
        }

        if (!merged) {
            if ((add || remove) && found != null) {
                count.decrementAndGet();
                onRemoved(found);
            }

            if (added != null) {
                count.incrementAndGet();
                onAdded(added);
            }
        }

        /*if (Param.DEBUG)*/
        {
            int cnt = count.get();
            if (cnt > capacity()) {
                throw new RuntimeException("overflow");
            } else if (cnt < 0) {
                throw new RuntimeException("underflow");
            }
        }


        return !add ? found : added;

    }

    /** can override in subclasses for custom replacement policy */
    protected boolean replace(float incomingPri, float existingPri) {
        return hijackSoftmax(incomingPri, existingPri);
    }

    /** can override in subclasses for custom equality test */
    protected boolean equals(Object x, X y) {
        return x == y || x.equals(y);
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
    public BLink<X> put(@NotNull X x, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflowing) {

        BLink<X> bb;
        if (b instanceof BLink) {
            bb = (BLink) b;
        } else {
            bb = newLink(x, null);
            bb.setBudget(b);
        }

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

    public void unbusy(int x, int ticket) {
        boolean freed = busy.remove(x, ticket);
        if (!freed)
            throw new RuntimeException("insertion fault");
    }

    /**
     * considers if this priority value stretches the current min/max range
     * this value will be updated for certain during a commit, so this value
     * only improves accuracy between commits.
     */
    private float range(float p) {
        if (p != p)
            return p;
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
    public @Nullable BLink<X> get(@NotNull Object key) {
        return update(key, null, 0);
    }

    @Override
    public int capacity() {
        return map.length();
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @Nullable
    @Override
    public Bag<X> sample(int n, @NotNull Predicate<? super BLink<X>> target) {
        if (isEmpty())
            return null;

        int c = capacity();
        int jLimit = (int) Math.ceil(c * SCAN_ITERATIONS);

        int i = random.nextInt(c), j = 0;

        int batchSize = n;

        float r = curve(); //randomized threshold

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        int di = random.nextBoolean() ? +1 : -1;

        //ArrayBLink<X> a = new ArrayBLink<>();

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {
            int m = ((i += di) & (c - 1));

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access
            BLink<X> v = map.get(m);

            if (v != null) {

                float p = v.pri();
                if (p == p) {
                    //if (p >= 0) {

                        if ((r < p) || (r < p + tolerance(j, jLimit, n, batchSize, c))) {
                            if (target.test(v)) {
                                n--;
                                r = curve();
                            }
                        }
                    //}
                } else {
                    //early deletion nullify
                    map.compareAndSet(m, v, null);
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

    @Override
    public void forEach(@NotNull Consumer<? super BLink<X>> e) {
        if (isEmpty())
            return;

        int c = capacity();

        int j = 0;

        while (j < c) {
            int m = ((j++) & (c - 1));


            BLink<X> v = map.get(m);

            if (v != null) {
                float p = v.pri();
                if (p == p) /* NaN? */ {
                    e.accept(v);
                } else {
                    map.compareAndSet(m, v, null);
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
        //throw new UnsupportedOperationException();
        return IntStream.range(0, capacity()).mapToObj(i -> map.get(i)).filter(n -> n != null).iterator();
    }

    @Override
    public boolean contains(@NotNull X it) {
        return get(it) != null;
    }

    @NotNull
    @Override
    public Bag<X> commit(Function<Bag, Consumer<BLink>> update) {
        final float[] mass = {0};

        final float[] min = {Float.POSITIVE_INFINITY};
        final float[] max = {Float.NEGATIVE_INFINITY};

        forEach((BLink<X> f) -> {
            float p = f.priSafe(0);

            if (p > max[0]) max[0] = p;
            if (p < min[0]) min[0] = p;
            mass[0] += p;
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
        if (each != null)
            forEach(each);

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
