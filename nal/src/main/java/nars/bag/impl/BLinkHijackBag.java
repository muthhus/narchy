package nars.bag.impl;

import nars.Param;
import nars.attention.Forget;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Consumer;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
public class BLinkHijackBag<K> extends BudgetHijackBag<K,BLink<K>> {

    public BLinkHijackBag(int capacity, int reprobes, BudgetMerge merge, Random random) {
        this(reprobes, merge, random);
        setCapacity(capacity);
    }

    public BLinkHijackBag(int reprobes, BudgetMerge merge, Random random) {
        super(random, merge, reprobes);
        this.map.set(EMPTY_ARRAY);
    }



    @Override
    protected Consumer<BLink<K>> forget(float rate) {
        return new Forget(rate);
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

    @Override
    protected float priEpsilon() {
        return Param.BUDGET_EPSILON;
    }

    @Override
    protected float temperature() {
        return Param.BAG_TEMPERATURE;
    }


}
