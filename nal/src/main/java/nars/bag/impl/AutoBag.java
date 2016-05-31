package nars.bag.impl;

import nars.NAR;
import nars.bag.Bag;
import nars.budget.forget.Forget;
import nars.link.BLink;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-tunes forgetting rate according to inbound demand, which is zero if bag is
 * under capacity.
 */
public class AutoBag<V>  {

    private final Forget.AbstractForget forget;

    public AutoBag(@NotNull MutableFloat perfection) {
        //this(new Forget.ExpForget(new MutableFloat(0), perfection));
        this(new Forget.LinearForget(new MutableFloat(0), perfection));
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
    public Bag<V> update(@NotNull Bag<V> bag, boolean forceCommit) {

        Forget.AbstractForget f;
        float r = forgetPeriod((ArrayBag<V>) bag);

        if (r > 0) {
            f = forget;
            /*if (bag.size() > 500)
                System.out.println(bag.size() + " / " + bag.capacity() + " pressurized_forgetRate="  + r);*/
            f.setForgetCycles(r);
        } else {
            if (!forceCommit)
                return bag;

            f = null;
        }

        return bag.commit(f);
    }


    protected float forgetPeriod(@NotNull ArrayBag<V> bag) {

        float basePeriod = 0.001f; //"margin of replacement"
        // TODO formalize some relationship between cycles and priority
        // TODO estimate based on the min/max priority of existing items and normalize the rate to that

        return (basePeriod) * bag.capacity()/bag.getPendingMass();

    }

    public final void update(NAR nar) {
        forget.update(nar);
    }

    @Deprecated public final void cycle(float subCycle) {
        forget.cycle(subCycle);
    }

}
