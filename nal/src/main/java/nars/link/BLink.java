package nars.link;

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

    public final void setPriority(float p, float now) {
        setPriority(p);
        setLastForgetTimeFast(now);
    }

    protected abstract void setLastForgetTimeFast(float now);


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
        Object x = get();
        return ((x == null) && (obj == null)) || ((obj instanceof BLink) && ((BLink) obj).get().equals(x));
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
