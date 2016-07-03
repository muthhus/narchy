package nars.bag.impl;

import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.forget.BudgetForget;
import nars.link.BLink;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-tunes forgetting rate according to inbound demand, which is zero if bag is
 * under capacity.
 */
public final class AutoBag<V> implements BudgetForget {
    private long now;
    private float ratio;



    public AutoBag() {
        //this(new Forget.ExpForget(new MutableFloat(0), perfection));
        //this(new Forget.PercentForget(new MutableFloat(0), perfection));
    }



    /** @param bag
     * @return
     */
    public Bag<V> commit(@NotNull Bag<V> bag) {

        ArrayBag<V> abag = (ArrayBag<V>) bag; //HACK

        synchronized (abag.map) {
            float r = forgetRatio(abag);
            this.ratio = r;

            return abag.commit(
                    (r >= Global.BUDGET_EPSILON) ?
                            this :
                            null /* no forgetting to be applied */
            );
        }

    }


    protected float forgetRatio(@NotNull ArrayBag<V> bag) {

        if (!bag.isFull()) {
            return -1f;
        }

        float[] b = bag.preCommit();

        float pending = b[1];
        if (pending <= Global.BUDGET_EPSILON) //TODO this threshold prolly can be increased some for more efficiency
            return -1f;
        float existing = b[0];

        // TODO formalize some relationship between cycles and priority

        //a load factor
        //final float massMeanTarget = 0.25f; //ex: 0.5 pri * 0.5 dur = 0.25 mass

        //float overflow = (existing+pending) - (massMeanTarget * bag.capacity());
        //if (overflow <= Global.BUDGET_EPSILON) //TODO this threshold prolly can be increased some for more efficiency
           //return -1;

//        float decaySpeed = Global.AUTOBAG_NOVELTY_RATE; //< 1.0, smaller means slower forgetting rate / longer forgetting time
//        float period = existing / (pending * decaySpeed);
//
//        //System.out.println("existing " + existing + " (est), pending: " + pending + " ==> " + overflow + " x " + bag.size() + " ==> " + period);
//
//        return period;

        //TODO consider age in diminishing existing's value
        float ratio = 1f - (existing / (existing + pending));
        //System.out.println("existing=" + existing + ", pending=" + pending + " .. ratio=" + ratio);
        return ratio;
    }

    @Override
    public final void update(NAR nar) {

        this.now = nar.time();
        //forget.update(nar);
    }

    @Override
    @Deprecated public final void cycle(float subCycle) {

        //forget.cycle(subCycle);
    }

    @Override
    public void accept(BLink bLink) {
        bLink.priMult( 1f - (ratio * (1f - bLink.dur()) ));
    }
}
