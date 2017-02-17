package nars.budget;

import org.jetbrains.annotations.NotNull;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted.
 */
public class DependentBLink<X extends Budgeted> extends RawBLink<X> {

    public DependentBLink(@NotNull X id) {
        super(id, id.priSafe(0), id.qua());
    }

    public DependentBLink(@NotNull X id, float p, float q) {
        super(id, p, q);
    }

    @Override
    public DependentBLink<X> cloneScaled(BudgetMerge merge, float scale) {

        if (scale!=1f) {
            DependentBLink<X> adding2 = new DependentBLink<X>(id, 0, qua()); //use the incoming quality.  budget will be merged
            merge.apply(adding2, this, scale);
            return adding2;
        } else {
            //return new DependentBLink(id, priSafe(0), qua());
            return this;
        }

    }

    @Override
    public DependentBLink<X> cloneZero(float q) {
        return new DependentBLink<>(id, 0, q);
    }

    @Override
    public final boolean isDeleted() {
        return super.isDeleted() || (id.isDeleted() && delete());
    }
}
