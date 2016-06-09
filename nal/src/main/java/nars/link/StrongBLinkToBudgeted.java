package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;

/**
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted
 */
public final class StrongBLinkToBudgeted<B extends Budgeted> extends StrongBLink<B> {

    public StrongBLinkToBudgeted(B id, @NotNull Budgeted b, float scal) {
        super(id, b, scal);
    }

    @Override
    public boolean commit() {
        return (get().isDeleted()) ? delete() : super.commit();
    }
}
