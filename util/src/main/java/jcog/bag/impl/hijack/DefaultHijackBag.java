package jcog.bag.impl.hijack;

import jcog.pri.PriReference;
import jcog.pri.op.PriForget;
import jcog.pri.op.PriMerge;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
public class DefaultHijackBag<K> extends PriorityHijackBag<K, PriReference<K>> {

    protected final PriMerge merge;

    public DefaultHijackBag(PriMerge merge, int capacity, int reprobes) {
        super(capacity, reprobes);
        this.merge = merge;
    }

    @Override
    protected PriReference<K> merge( PriReference<K> existing,  PriReference<K> incoming, MutableFloat overflowing) {
        float overflow = merge.merge(existing, incoming); //modify existing
        if (overflow > 0) {
            //pressurize(-overflow);
            if (overflowing!=null) overflowing.add(overflow);
        }
        return existing;
    }




    @Override
    protected Consumer<PriReference<K>> forget(float avgToBeRemoved) {
        return new PriForget(avgToBeRemoved);
    }


    @Override
    public K key(PriReference<K> value) {
        return value.get();
    }


}

//    public static void flatForget(BudgetHijackBag<?,? extends Budgeted> b) {
//        double p = b.pressure.get() /* MULTIPLIER TO ANTICIPATE NEXT period */;
//        int s = b.size();
//
//
//            //float ideal = s * b.temperature();
//            if (p > Param.BUDGET_EPSILON * s) {
//                if (b.pressure.compareAndSet(p, 0)) {
//
//                    b.commit(null); //precommit to get accurate mass
//                    float mass = b.mass;
//
//                    float over = //(float) ((p + mass) - ideal);
//                            ((float) p / ((float)p + mass) / s);
//                    if (over >= Param.BUDGET_EPSILON) {
//                        b.commit(x -> x.budget().priSub(over * (1f - x.qua())));
//                    }
//                }
//
//            }
//
//    }
