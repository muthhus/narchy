package nars.link;

import nars.budget.Budget;
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

    public DependentBLink(@NotNull B id, Budgeted b) {
        super(id, b);
    }


    @Override
    public boolean isDeleted() {
        B x = id;

        if (x!=null) {
            boolean deleting = false;

            if (x.isDeleted()) {
                priority = Float.NaN;
                deleting = true;
            }
            if (super.isDeleted()) {
                deleting = true;
            }

            if (deleting) {
                id = x = null;
            }

            return deleting;

        } else {
            return true;
        }

    }
}
