package nars.bag.impl;

import jcog.bag.impl.HijackBag;
import nars.Task;
import nars.budget.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Created by me on 2/17/17.
 */
abstract public class BudgetHijackBag<K,V extends Budgeted> extends HijackBag<K, V> {

    protected final BudgetMerge merge;

    public BudgetHijackBag(Random random, BudgetMerge merge, int reprobes) {
        super(random, reprobes);
        this.merge = merge;
    }

    @Override
    protected float merge(@Nullable V existing, @NotNull V incoming, float scale) {
        Budget applied;
        if (existing == null) {
            if (scale == 1)
                return incoming.priSafe(0); //nothing needs done

            existing = incoming;
            applied = new RawBudget(0, existing.qua() );
            scale = 1f - scale;
        } else {
            applied = incoming.budget();
        }

        float pBefore = priSafe(existing, 0);
        merge.apply(existing.budget(), applied, scale); //TODO overflow
        pressure += priSafe(existing, 0) - pBefore;

        return pressure;
    }



}
