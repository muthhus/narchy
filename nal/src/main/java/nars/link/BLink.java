package nars.link;

import com.google.common.base.Objects;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.RawBudget;
import nars.budget.UnitBudget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by me on 5/29/16.
 */
public abstract class BLink<X> extends RawBudget implements Link<X> {

    final static AtomicInteger serial = new AtomicInteger();

    final int hash = serial.incrementAndGet();

    @Override abstract public X get();
    abstract public void set(X x);

    @Override
    public boolean isDeleted() {
        float p = pri(); //b[PRI];
        return (p!=p); //fast NaN test
    }

    @Override
    public @NotNull UnitBudget clone() {
        return new UnitBudget(this);
    }

    @Override
    public final boolean equals(Object that) {
        return this == that;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    //    @Override public final boolean equals(Object obj) {
////        /*if (obj instanceof Budget)*/ {
////            return equalsBudget((Budget) obj);
////        }
////        return id.equals(((BagBudget)obj).id);
//        boolean result;
//        @Nullable X x = get();
//        if (obj instanceof BLink) {
//            Object o = ((BLink) obj).get();
//            result = Objects.equal(x, o);
//        } else {
//            result = Objects.equal(x, obj);
//        }
//        return obj == this || result;
//    }
//
//
//    @Override public final int hashCode() {
//        Object x = get();
//        return x == null ? 0 : x.hashCode();
//    }

    @NotNull
    @Override
    public String toString() {
        return get() + "=" + getBudgetString();
    }


    public void set(X nx, Budgeted b, float scale) {
        set(nx);
        budget(b.pri()*scale, b.dur(), b.qua());
    }
}
