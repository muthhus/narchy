package nars.link;

import nars.budget.Budgeted;
import nars.budget.RawBudget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 *
 * UNTESTED EXPERIMENTAL
 *
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
public class WeakBLink<X> extends RawBudget implements BLink<X> {


    ///** the referred item */
    @NotNull protected final WeakReference<X> id;
    private final int hash;

    public WeakBLink(@NotNull X id) {
        this.hash = id.hashCode();
        this.id = new WeakReference<>(id);
        this.priority = Float.NaN; //begin deleted
    }

    @Override
    public final boolean equals(@NotNull Object that) {
        if (this==that)
            return true;

        Object b = ((BLink) that).get();
        return b!=null && b.equals(get());
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public @NotNull String toString() {
        return get() + "=" + super.toString();
    }

    @Override
    public boolean delete() {
        float p = this.priority;
        if (p==p) {
            //not already deleted
            id.clear();
            this.priority = (Float.NaN);
            return true;
        }
        return false;
    }

    @Override
    public final boolean isDeleted() {
        if (super.isDeleted())
            return true;

        X x = get();
        if (x == null) {
            delete();
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public final X get() {
        return id.get();
    }

}
