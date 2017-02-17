package nars.link;

import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.budget.RawBudget;
import org.jetbrains.annotations.NotNull;

/**
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
public class RawBLink<X> extends RawBudget implements BLink<X> {


    ///** the referred item */
    @NotNull protected final X id;

    public RawBLink(@NotNull X id) {
        this.id = id;
        this.quality = Float.NaN; //begin 'new'
    }

    public RawBLink(@NotNull X id, float p, float q) {
        this.id = id;
        this.priority = p;
        this.quality = q;
    }

    public RawBLink(@NotNull X id, @NotNull Budgeted b) {
        this(id, b.priSafe(0), b.qua());
    }

    public RawBLink(@NotNull X id, @NotNull Budgeted b, float scale) {
        this(id, b.pri() * scale, b.qua());
    }

    @Override
    public RawBLink<X> cloneScaled(BudgetMerge merge, float scale) {
        if (scale!=1f) {
            RawBLink<X> adding2 = new RawBLink<X>(id, 0, qua()); //use the incoming quality.  budget will be merged
            merge.apply(adding2, this, scale);
            return adding2;
        } else {
            //return new RawBLink(id, priSafe(0), qua());
            return this;
        }
    }

    @Override
    public RawBLink<X> cloneZero(float q) {
        return new RawBLink<>(id, 0, q);
    }

    @Override
    public final boolean equals(@NotNull Object that) {
        if (this==that)
            return true;

        Object b = ((BLink) that).get();
        return this.id.equals(b);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return id + "=" + super.toString();
    }

    @Override
    public boolean delete() {
        float p = this.priority;
        if (p==p) {
            //not already deleted
            this.priority = (Float.NaN);
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public final X get() {
        return id;
    }

}
