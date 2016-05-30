package nars.link;

import nars.budget.Budgeted;

/**
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted
 */
public final class WeakBLinkToBudgeted<B extends Budgeted> extends WeakBLink<B> {

    public WeakBLinkToBudgeted(B id, Budgeted b, float scal) {
        super(id, b, scal);
    }

    @Override
    public boolean commit() {
        B val = id.get();
        return (val == null || val.isDeleted()) ? delete() : super.commit();
    }
}
