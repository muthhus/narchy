package nars.link;

import nars.budget.Budgeted;
import nars.budget.RawBudget;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
public class DefaultBLink<X> extends RawBudget implements BLink<X> {


    ///** the referred item */
    @NotNull protected final X id;

    public DefaultBLink(@NotNull X id) {
        this.id = id;
    }

    public DefaultBLink(@NotNull X id, float p, float d, float q) {
        setBudget(p, d, q);
        this.id = id;
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b) {
        this(id, b, 1f);
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b, float scale) {
        this(id, b.pri() * scale, b.dur(), b.qua());
    }

    @Override
    public boolean isDeleted() {
        return id == null || super.isDeleted();
    }

    @Override
    public final boolean equals(@NotNull Object that) {
        return Objects.equals(id, ((BLink)that).get());
    }

    @Override
    public final int hashCode() {
        return id!=null ? id.hashCode() : 0;
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
