package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

/**
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted
 */
public final class StrongBLinkToBudgeted<B extends Budgeted> extends StrongBLink<B> {


    public StrongBLinkToBudgeted(B id, float p, float d, float q) {
        super(id, p, d, q);
    }

    @Override
    public boolean commit() {
        return (get().isDeleted()) ? delete() : super.commit();
    }
}
