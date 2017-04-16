package nars.bag.impl;

import jcog.bag.Priority;
import jcog.bag.impl.HijackBag;
import nars.budget.BudgetMerge;
import nars.budget.RawBudget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Created by me on 2/17/17.
 */
abstract public class BudgetHijackBag<K,V extends Priority> extends HijackBag<K, V> {

    protected final BudgetMerge merge;

    public BudgetHijackBag(Random random, BudgetMerge merge, int reprobes) {
        super(reprobes, random);
        this.merge = merge;
    }

    @Override
    protected float merge(@Nullable V existing, @NotNull V incoming, float scale) {
        float inPri = incoming.priSafe(0);
        float pressure = inPri * scale;
        Priority applied;
        if (existing == null) {
            existing = incoming;
            applied = new RawBudget(0 );
            scale = 1f - scale; //?? does this actually work
        } else {
            applied = incoming;
        }

        //float pBefore = priSafe(existing, 0);
        merge.apply(existing, applied, scale); //TODO overflow
        //return priSafe(existing, 0) - pBefore;

        return pressure;
    }



}
