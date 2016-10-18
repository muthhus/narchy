package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted.
 */
public class DependentBLink<B extends Budgeted> extends DefaultBLink<B> {

    public DependentBLink(@NotNull B id) {
        super(id);
    }

    public DependentBLink(@NotNull B id, @NotNull Budgeted b) {
        super(id, b);
    }


    @Override
    public final boolean isDeleted() {
        return super.isDeleted() || (id.isDeleted() && delete());
    }
}
