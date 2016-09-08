package nars.link;

import nars.budget.Budgeted;
import nars.budget.RawBudget;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
abstract public class DefaultBLink<X> extends RawBudget implements BLink<X> {

    final static AtomicInteger serial = new AtomicInteger();

    final int hash = serial.incrementAndGet();

    ///** the referred item */
    public X id;

    public DefaultBLink(@NotNull X id, float p, float d, float q) {
        budget(p, d, q);
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b) {
        this(id, b, 1f);
    }

    public DefaultBLink(@NotNull X id, @NotNull Budgeted b, float scale) {
        budget(b.pri() * scale, b.dur(), b.qua());
    }

    @Override
    public boolean isDeleted() {
        return id == null || super.isDeleted();
    }

    @Override
    public final boolean equals(Object that) {
        return this == that;
    }

    @Override
    public final int hashCode() {
        return hash;
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
            this.id = null;
            return true;
        }
        return false;
    }


}
