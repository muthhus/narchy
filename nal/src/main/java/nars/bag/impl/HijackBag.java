package nars.bag.impl;

import nars.attention.Forget;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.budget.RawBLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
public class HijackBag<K> extends AbstractHijackBag<K,BLink<K>> {

    protected final BudgetMerge merge;

    public HijackBag(int capacity, int reprobes, BudgetMerge merge, Random random) {
        this(reprobes, merge, random);
        setCapacity(capacity);
    }

    public HijackBag(int reprobes, BudgetMerge merge, Random random) {
        super(random, reprobes);
        this.merge = merge;
        this.map.set(EMPTY_ARRAY);
    }


    @Override
    protected float merge(@Nullable BLink<K> existing, @NotNull BLink<K> incoming, float scale) {
        if (existing == null) {
            if (scale == 1)
                return incoming.priSafe(0); //nothing needs done

            existing = incoming;
            incoming = new RawBLink(existing.get(), 0, existing.qua() );
            scale = 1f - scale;
        }

        float pBefore = priSafe(existing, 0);
        merge.apply(existing, incoming, scale); //TODO overflow
        pressure += priSafe(existing, 0) - pBefore;

        return pressure;
    }

    @Override
    protected Forget<K> forget(float rate) {
        return new Forget<>(rate);
    }

    @Override
    public void onRemoved(@NotNull BLink<K> v) {
        v.delete();
    }

    @Override
    public float pri(@NotNull BLink<K> key) {
        return key.pri();
    }

    @Override
    public K key(BLink<K> value) {
        return value.get();
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
