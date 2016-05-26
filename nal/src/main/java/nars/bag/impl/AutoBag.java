package nars.bag.impl;

import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.forget.BudgetForget;
import nars.budget.forget.Forget;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-tunes forgetting rate according to inbound demand, which is zero if bag is
 * under capacity.
 */
public class AutoBag<V>  {

    private final Forget.AbstractForget forget;

    public AutoBag(MutableFloat perfection) {
        this(new Forget.ExpForget(new MutableFloat(0), perfection));
    }

    public AutoBag(Forget.AbstractForget forget) {
        this.forget = forget;
    }


    /** @param forceCommit - force a commit even if there are no pending items requiring
    *                      forgetting. this is necessary if the bag involves weak BLink's or other
     *                     links which can spontaneously get deleted or delete themselves and need
     *                     to be removed.
     * @param bag
     * @param forceCommit
     * @return
     */
    public Bag<V> update(Bag<V> bag, boolean forceCommit) {

        BudgetForget f;
        float r = forgetPeriod((ArrayBag<V>) bag);
        if (r > 0) {
            //forget.forgetDurations.setValue(r); //not necessary unless we want access to this value as a MutableFloat from elsewhere
            forget.setForgetCycles(r);
            f = forget;
        } else {
            if (!forceCommit)
                return bag;

            f = null;
        }

        return bag.commit(f);
    }


    protected float forgetPeriod(ArrayBag<V> bag) {
        if (!bag.isFull())
            return 0;

        FasterList<BLink<V>> pending = bag.pending;

        float pendingMass = 0;
        for (int i = 0, pendingSize = pending.size(); i < pendingSize; i++) {
            BLink<V> v = pending.get(i);
            pendingMass += v.pri() * v.dur();
        }

        float basePeriod = 1f; //TODO formalize some relationship between cycles and priority

        return (pendingMass/basePeriod) * bag.capacity();

    }

    public final void update(NAR nar) {
        forget.update(nar);
    }

    public final void cycle(float subCycle) {
        forget.cycle(subCycle);
    }

}
