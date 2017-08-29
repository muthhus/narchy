package jcog.pri;

import org.jetbrains.annotations.NotNull;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted.
 */
public class PLinkUntilDeleted<X extends Deleteable> extends PLink<X> {

    public PLinkUntilDeleted(@NotNull X id, float p) {
        super(id, p);
    }

    /** duplicate of Prioritized's impl, for speed (hopefully) */
    @Override public float priElseNeg1() {
        float p = pri(); //pri() for this subclass
        return p == p ? p : -1;
    }
    /** duplicate of Prioritized's impl, for speed (hopefully) */
    @Override public float priElseZero() {
        float p = pri(); //pri() for this subclass
        return p == p ? p : 0;
    }

    @Override
    public final float pri() {
        float p = this.pri;
        if (p == p) {
            if (id.isDeleted()) {
                return (this.pri = Float.NaN);
            } else {
                return p;
            }
        } else {
            return Float.NaN;
        }
    }


}
