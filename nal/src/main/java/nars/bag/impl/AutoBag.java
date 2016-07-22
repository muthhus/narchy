package nars.bag.impl;

import nars.NAR;
import nars.Param;
import nars.bag.Bag;
import nars.budget.forget.BudgetForget;
import nars.link.BLink;
import nars.nar.util.DefaultCore;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-tunes forgetting rate according to inbound demand, which is zero if bag is
 * under capacity.
 */
public final class AutoBag<V> implements BudgetForget {

    volatile private float ratio;

    /** prevents durabilty=1.0 from avoiding forgetting, a value ~1.0 */
    final static float maxEffectiveDurability = 1f;


    public AutoBag() {
        //this(new Forget.ExpForget(new MutableFloat(0), perfection));
        //this(new Forget.PercentForget(new MutableFloat(0), perfection));
    }

    /** @param bag
     * @return
     */
    public Bag<V> commit(@NotNull Bag<V> bag) {

        if (!(bag instanceof ArrayBag))
            return bag;

        ArrayBag<V> abag = (ArrayBag<V>) bag; //HACK

        synchronized (abag.map) {
            float r = -1f;

            if (!abag.pending.isEmpty()) { //(((ArrayBag) bag).pending.isFull()) {

                float[] b = abag.preCommit();

                float pending = b[1];

                if (pending > Param.BUDGET_EPSILON) { //TODO this threshold prolly can be increased some for more efficiency

                    float existing = b[0];

                    // TODO formalize some relationship between cycles and priority

                    //TODO consider age in diminishing existing's value
                    r = 1f - (existing / (existing + pending));

//                    if (abag instanceof DefaultCore.MonitoredCurveBag) {
//                        System.out.println(Texts.n4(abag.priHistogram(10)) + " " + r + " " + pending);
//                    }
                    //System.out.println("existing=" + existing + ", pending=" + pending + " .. ratio=" + ratio);

                }
            }

            this.ratio = r;


            return abag.commit(
                    (r >= Param.BUDGET_EPSILON) ?
                            this :
                            null /* no forgetting to be applied */
            );
        }

    }

    @Override
    public final void accept(@NotNull BLink bLink) {

        float eDur = bLink.dur() * maxEffectiveDurability;
        bLink.priMult( 1f - (ratio * (1f - eDur) ));
    }
}
