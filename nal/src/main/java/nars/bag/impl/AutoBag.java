package nars.bag.impl;

import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.forget.BudgetForget;
import nars.budget.forget.Forget;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Auto-tunes forgetting rate according to inbound demand, which is zero if bag is
 * under capacity.
 */
public class AutoBag<V>  {

    private final Forget.AbstractForget forget;
    private final ArrayBag<V> bag;

    public AutoBag(ArrayBag<V> bag) {
        this(bag, new Forget.ExpForget(new MutableFloat(0), new MutableFloat(0)));
    }

    public AutoBag(ArrayBag<V> bag, Forget.AbstractForget forget) {
        this.bag = bag;
        this.forget = forget;
    }

    public Bag<V> autocommit(NAR nar) {

        BudgetForget f;
        float r = forgetPeriod();
        if (r > 0) {
            forget.forgetDurations.setValue(r);
            forget.update(nar);
            f = forget;
        } else {
            f = null;
        }

        return bag.commit(f);
    }


    protected float forgetPeriod() {
        if (!bag.isFull())
            return 0;

        FasterList<BLink<V>> pending = bag.pending;

        float pendingMass = 0;
        for (int i = 0, pendingSize = pending.size(); i < pendingSize; i++) {
            BLink<V> v = pending.get(i);
            pendingMass += v.pri() * v.dur();
        }

        float baseRate = 1f;

        return (pendingMass/baseRate) * bag.capacity();

    }

}
