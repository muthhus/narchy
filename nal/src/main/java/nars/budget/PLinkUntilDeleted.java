package nars.budget;

import jcog.pri.Deletes;
import jcog.pri.RawPLink;
import org.jetbrains.annotations.NotNull;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted.
 */
public class PLinkUntilDeleted<X extends Deletes> extends RawPLink<X> {

    public PLinkUntilDeleted(@NotNull X id, float p) {
        super(id, p);
    }


    @Override
    public float pri() {
        float p = this.pri;
        if (p == p && id.isDeleted()) {
            return (this.pri = Float.NaN);
        } else {
            return p;
        }
    }


}
