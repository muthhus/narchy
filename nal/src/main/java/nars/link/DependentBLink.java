package nars.link;

import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.budget.Prioritized;
import org.jetbrains.annotations.NotNull;

/**
 * A BLink that references and depends on another Budgeted item (ex: Task).
 * Adds an additional condition that deletes the link if the referenced
 * Budgeted is deleted.
 */
public class DependentBLink<X extends Prioritized> extends RawBLink<X> {

    public DependentBLink(@NotNull X id) {
        super(id);
        setPriority(id.pri());
        if (id instanceof Budgeted)
            setQuality(((Budgeted)id).qua());
    }
    public DependentBLink(@NotNull X id, float p, float q) {
        super(id);
        setBudget(p, q);
    }

    @Override
    public DependentBLink<X> cloneScaled(BudgetMerge merge, float scale) {
        DependentBLink<X> adding2 = new DependentBLink<X>(id);
        adding2.setBudget(0, qua()); //use the incoming quality.  budget will be merged
        merge.apply(adding2, this, scale);
        return adding2;
    }

    @Override
    public DependentBLink<X> cloneZero() {
        return new DependentBLink<>(id, 0, Float.NaN);
    }

    @Override
    public final boolean isDeleted() {
        return super.isDeleted() || (id.isDeleted() && delete());
    }
}
