package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

import static nars.util.Util.clamp;

/**
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
abstract public class DefaultBLink<X> extends BLink<X> {

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
