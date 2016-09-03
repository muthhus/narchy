package nars.link;

import com.google.common.base.Objects;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.UnitBudget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/29/16.
 */
public abstract class BLink<X> implements Budget, Link<X> {


    @Override abstract public X get();

    protected void init(@NotNull Budgeted c, float scale) {
        init(c.pri() * scale, c.dur(), c.qua());
    }

    abstract public void init(float p, float d, float q);

    @Override
    public boolean isDeleted() {
        float p = pri(); //b[PRI];
        return (p!=p); //fast NaN test
    }

    @Override
    public @NotNull UnitBudget clone() {
        return new UnitBudget(this);
    }


    @Override public final boolean equals(Object obj) {
//        /*if (obj instanceof Budget)*/ {
//            return equalsBudget((Budget) obj);
//        }
//        return id.equals(((BagBudget)obj).id);
        boolean result;
        @Nullable X x = get();
        if (obj instanceof BLink) {
            Object o = ((BLink) obj).get();
            result = Objects.equal(x, o);
        } else {
            result = Objects.equal(x, obj);
        }
        return obj == this || result;
    }


    @Override public final int hashCode() {
        Object x = get();
        return x == null ? 0 : x.hashCode();
    }

    @NotNull
    @Override
    public String toString() {
        return get() + "=" + getBudgetString();
    }


}
