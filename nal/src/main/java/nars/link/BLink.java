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
public abstract class BLink<X> extends Budget implements Link<X> {



    @Nullable
    @Override abstract public X get();

    protected void init(@NotNull Budgeted c, float scale) {
        init(c.pri() * scale, c.dur(), c.qua());
    }

    abstract public void init(float p, float d, float q);

    abstract public float priDelta();

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
        return obj == this || equalsReferenced(obj);
    }

    private final boolean equalsReferenced(@Nullable Object obj) {
        @Nullable X x = get();
        if (obj instanceof BLink) {
            Object o = ((BLink) obj).get();
            return Objects.equal(x, o);
        } else {
            return Objects.equal(x, obj);
        }
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


    public abstract boolean commit();

    /** show additional information */
    public String toString2() {
        return toString();
    }

}
