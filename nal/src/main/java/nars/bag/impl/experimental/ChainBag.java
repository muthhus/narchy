package nars.bag.impl.experimental;

import nars.util.data.linkedlist.DD;
import nars.util.data.linkedlist.DDList;
import nars.util.data.linkedlist.DDNodePool;
import nars.util.math.Distributor;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ChainBag repeatedly cycles through a linked list containing
 * the set of items, stored in an arbitrary order.
 * <p>
 * Probabalistic selection is decided according to a random function
 * of an item's priority, with options for normalizing against
 * the a priority range encountered in a sliding window.
 * <p>
 * This allows it to maximize the dynamic range across the bag's contents
 * regardless of their absolute priority distribution (percentile vs.
 * percentage).
 * <p>
 * Probability can be further weighted by a curve function to
 * fine-tune behavior.
 */
public class ChainBag<V> {


    private final transient Mean mean; //priority mean, continuously calculated
    private Random rng;

    private boolean ownsNodePool = false;

    private int capacity;

    transient DD<V> current = null;

    //public transient Frequency removal = new Frequency();

    float minMaxMomentum = 0.98f;

    private final transient DDNodePool<V> nodePool;

    DD<V> nextRemoval = null;


    /**
     * mapping from key to item
     */
    public final Map<V, DD<V>> index;

    /**
     * array of lists of items, for items on different level
     */
    public final DDList<V> chain;

    private static final float PERCENTILE_THRESHOLD_FOR_EMERGENCY_REMOVAL = 0.5f; //slightly below half
    private float estimatedMax = 0.5f;
    private float estimatedMin = 0.5f;
    private float estimatedMean = 0.5f;

    final short[] d;
    int dp = 0;
    private float searchFactor = 0.25f;


    public ChainBag(Random rng, DDNodePool<V> nodePool, int capacity) {

        d = Distributor.get((int) (Math.sqrt(capacity))).order;

        this.rng = rng;
        this.capacity = capacity;
        this.index = new ConcurrentHashMap<>(capacity);


        this.nodePool = nodePool;

        chain = new DDList(0, nodePool);
        mean = new Mean();
    }


    public ChainBag(Random rng, int capacity) {
        this(rng, new DDNodePool(capacity + 1), capacity);
        ownsNodePool = true;
    }

    public int capacity() {
        return capacity;
    }


    public V pop() {
        if (size() == 0) return null;
        DD<V> d = next(true);
        if (d == null) return null;

        //removal.addValue(d.item.getPriority());
        V v = remove(d.item);

        //if ($.DEBUG) validate();

        return v;
    }

    public V peekNext() {
        DD<V> d = next(true);
        if (d != null) return d.item;
        return null;
    }

    //TODO handle Deleted items like Bag.update(..)
//    
//    public V update(BagTransaction<K, V> selector) {
//
//        final K key = selector.name();
//        final DD<V> bx;
//        if (key != null) {
//            bx = index.get(key);
//        }
//        else {
//            bx = next(true);
//        }
//
//        if ((bx == null) || (bx.item == null)) {
//            //allow selector to provide a new instance
//            V n = selector.newItem();
//            if (n!=null) {
//                return putReplacing(n, selector);
//            }
//            //no instance provided, nothing to do
//            return null;
//        }
//
//        final V b = bx.item;
//
//        //allow selector to modify it, then if it returns non-null, reinsert
//
//        if (!b.getBudget().isDeleted())
//            temp.budget( b.getBudget() );
//        else
//            temp.zero();
//
//        final Budget c = selector.updateItem(b, temp);
//        if ((c!=null) && (!c.equalsByPrecision(b.getBudget()))) {
//            b.getBudget().budget(c);
//            updatePercentile(b.getPriority());
//        }
//
//        return b;
//
//    }


    public void put(V newItem) {


        DD<V> previous = index.get(newItem);
        if (previous != null) {

            //displaced an item with the same key
            V previousItem = previous.item;
            merge(newItem, previousItem);

            updatePercentile(weigh(previousItem));

            return;
        }

        DD<V> d;
        synchronized (chain) {
            DD<V> prev = index.put(newItem, d = chain.add(newItem));
            if (prev!=null)
                chain.remove(prev);
            int cap = capacity();
            while (size() > cap) {
                removeNext(1);
            }
        }

        updatePercentile(weigh(newItem));

    }

    private void removeNext(int i) {

        while (i > 0) {

            removeFirst();
            //removeNextPercentile();
            i--;
        }

    }

    public boolean isEmpty() {
        return size() == 0;
    }

    private V removeFirst() {

        DD<V> first;
        synchronized (chain) {
            first = chain.getFirstNode();
        }
        if (first == null)
            return null;

        return remove(first.item);
    }



    private void removeNextPercentile() {
        if (nextRemoval == null) {
            //find something to remove
            getNextRemoval();
        }

        V overflow = remove(nextRemoval.item);
        if (overflow == null) {
            //TODO not sure why this happens
            //if ($.DEBUG) validate();
            throw new RuntimeException(this + " error removing nextRemoval=" + nextRemoval);
        }

        nextRemoval = null;
    }

    private float weigh(V newItem) {
        return 1f;
    }

    protected void merge(V newItem, V existing) {

    }

    protected void getNextRemoval() {
        int size = size();
        if (size == 0) return;

        int loops = 0;

        DD<V> c = current; //save current position

        float searchDepth = size * searchFactor;

        while (nextRemoval == null && loops++ <= searchDepth)
            next(false);

        if (nextRemoval == null) {
            //throw new RuntimeException(this + " considered nothing removeable");
            nextRemoval = current;
        }

        current = c;  //restore current position if it wasn't what was removed
    }

    /**
     * @param byPriority - whether to select according to priority, or just the next item in chain order
     * @return
     */
    protected DD<V> next(boolean byPriority) {
        int s = size();
        if (s == 0) return null;
        //final boolean atCapacity = s >= capacity();

        DD<V> next = after(current);

        if (s == 1)
            return next;

        do {


            /*if (next == null) {
                throw new RuntimeException("size = " + size() + " yet there is no first node in chain");
            }*/

            V ni = next.item;

            /*if (ni == null) {
                throw new RuntimeException("size = " + size() + " yet iterated cell with null item");
            }*/

            double percentileEstimate = getPercentile(weigh(ni));


            if (!byPriority) {
                if (nextRemoval == null)
                    considerRemoving(next, percentileEstimate);
                break;
            }
            if (selectPercentile(percentileEstimate))
                break;

            considerRemoving(next, percentileEstimate);

            next = after(next);

        } while (true);

        return current = next;
    }


    public void setCapacity(int c) {
        capacity = c;
    }


    /**
     * updates the adaptive percentile measurement; should be called on put and when budgets update
     */
    private void updatePercentile(float priority) {
        //DescriptiveStatistics percentile is extremely slow
        //contentStats.getPercentile(ni.getPriority())
        //approximate percentile using max/mean/min

        mean.increment(priority);
        float mean = (float) this.mean.getResult();

        float momentum = minMaxMomentum;


        estimatedMax = (estimatedMax < priority) ? priority : (1.0f - momentum) * mean + (momentum) * estimatedMax;
        estimatedMin = (estimatedMin > priority) ? priority : (1.0f - momentum) * mean + (momentum) * estimatedMin;
        estimatedMean = mean;
    }

    /**
     * uses the adaptive percentile data to estimate a percentile of a given priority
     */
    private double getPercentile(float priority) {

        float mean = estimatedMean;

        float upper, lower;
        if (priority < mean) {
            lower = estimatedMin;
            upper = mean;
        } else if (priority == mean) {
            return 0.5f;
        } else {
            upper = estimatedMax;
            lower = mean;
        }

        float perc = (priority - lower) / (upper - lower);

        float minPerc = 1.0f / size();

        if (perc < minPerc) return minPerc;

        return perc;
    }

    protected boolean considerRemoving(DD<V> d, double percentileEstimate) {
        //TODO improve this based on adaptive statistics measurement
        V item = d.item;
        float p = weigh(item);
        DD<V> nr = nextRemoval;
        if (nr == null) {
            if (percentileEstimate <= PERCENTILE_THRESHOLD_FOR_EMERGENCY_REMOVAL) {
                nextRemoval = d;
                return true;
            }
        } else if (nr != d) {
            if (p < weigh(nr.item)) {
                nextRemoval = d;
                return true;
            }
        }

        return false;
    }

    protected boolean selectPercentile(double percentileEstimate) {
        //return selectPercentileRandom(percentileEstimate);
        return selectPercentileDistributor(percentileEstimate);
    }

    protected boolean selectPercentileDistributor(double percentileEstimate) {
        int dLen = d.length;
        return d[(dp++) % dLen] / ((double) dLen) < (percentileEstimate);
    }

    protected boolean selectPercentileRandom(double percentileEstimate) {
        return rng.nextFloat() < percentileEstimate;
    }

    protected boolean selectPercentage(V v) {
        return rng.nextFloat() < weigh(v);
    }

    protected DD<V> after(DD<V> d) {
        DD<V> n = d != null ? d.next : null;
        if ((n == null) || (n.item == null)) {
            synchronized (chain) {
                return chain.getFirstNode();
            }
        }
        return n;
    }


    public int size() {
        return index.size();
    }

    public int validatedSize() {
        int s1 = index.size();
        //int s2 = chain.size();
        /*if (s1 != s2)
            throw new RuntimeException(this + " bag fault; inconsistent index (" + s1 + " index != " + s2 + " chain)");*/
        int cap = capacity();
        if (s1 > cap + cap / 2) {
            String message = this + " has exceeded capacity: " + s1 + " > " + capacity();
            //throw new RuntimeException(message);
            System.err.println(message);
        }

        return s1;
    }


//    public Iterator<V> iterator() {
//        return chain.iterator();
//    }


    public void clear() {

        index.clear();

        synchronized (chain) {
            chain.clear();
        }

        current = null;
        estimatedMin = estimatedMax = estimatedMean = 0.5f;

    }


//    public void delete() {
//
//        if (ownsNodePool)
//            nodePool.delete();
//
//        index.clear();
//        chain.delete();
//
//    }

    public V remove(V key) {
        DD<V> d = index.remove(key);
        if (d != null) {
            V v = d.item; //save it here because chain.remove will nullify .item field
            if (key == v) {
                removeFromChain(d);
            } else {
                throw new RuntimeException("chain bag fault");
            }

            return v;
        } else {
            return null;
        }
    }

    public void removeFromChain(DD<V> d) {
        synchronized (chain) {
            chain.remove(d);

            if (current == d)
                current = after(current);
        }
    }


    public V get(V key) {
        DD<V> d = index.get(key);
        return (d != null) ? d.item : null;
    }

    public void forEach(Consumer<? super V> value) {
        synchronized (chain) {
            chain.forEach(value);
        }
    }
}
