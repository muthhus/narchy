package nars.budget;

import org.jetbrains.annotations.NotNull;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted.
 */
public class DependentBLink<X extends Budgeted> extends RawBLink<X> {

    public DependentBLink(@NotNull X id) {
        super(id, id.priSafe(0));
    }

    public DependentBLink(@NotNull X id, float p, float q) {
        super(id, p);
    }


    @Override
    public final boolean isDeleted() {
        return super.isDeleted() || (id.isDeleted() && delete());
    }
}
