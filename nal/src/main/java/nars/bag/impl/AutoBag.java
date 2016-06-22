package nars.bag.impl;

import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.forget.Forget;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-tunes forgetting rate according to inbound demand, which is zero if bag is
 * under capacity.
 */
public final class AutoBag<V>  {

    private final Forget.AbstractForget forget;


    public AutoBag(@NotNull MutableFloat perfection) {
        //this(new Forget.ExpForget(new MutableFloat(0), perfection));
        this(new Forget.LinearForget(new MutableFloat(0), perfection));
    }

    public AutoBag(Forget.AbstractForget forget) {
        this.forget = forget;
    }


    /** @param bag
     * @return
     */
    public Bag<V> commit(@NotNull Bag<V> bag) {

        ArrayBag<V> abag = (ArrayBag<V>) bag; //HACK

        synchronized (abag.map) {
            float r = forgetPeriod(abag);

            return abag.commit(
                    (r > 0 && r <= Global.maxForgetPeriod) ?
                            forget.setForgetCycles(Math.max(Global.minForgetPeriod, r)) :
                            null /* no forgetting to be applied */
            );
        }

    }


    protected float forgetPeriod(@NotNull ArrayBag<V> bag) {

        float[] b = bag.preCommit();

        float pending = b[1];
        if (pending <= Global.BUDGET_EPSILON) //TODO this threshold prolly can be increased some for more efficiency
            return -1f;
        float existing = b[0];

        // TODO formalize some relationship between cycles and priority

        //a load factor
        final float massMeanTarget = 0.5f; //ex: 0.5 pri * 0.5 dur = 0.25 mass

        float overflow = (existing+pending) - (massMeanTarget * bag.capacity());
        if (overflow <= Global.BUDGET_EPSILON) //TODO this threshold prolly can be increased some for more efficiency
            return -1;

        float decaySpeed = 0.5f; //< 1.0, smaller means slower forgetting rate / longer forgetting time
        float period = existing / (overflow * decaySpeed);

        System.out.println("existing " + existing + " (est), pending: " + pending + " ==> " + overflow + " x " + bag.size() + " ==> " + period);

        return period;

    }

    public final void update(NAR nar) {
        forget.update(nar);
    }

    @Deprecated public final void cycle(float subCycle) {
        forget.cycle(subCycle);
    }

}
