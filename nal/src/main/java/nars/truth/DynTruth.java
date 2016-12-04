package nars.truth;

import nars.*;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.dynamic.DynamicBeliefTask;
import nars.nal.Stamp;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.CONJ;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth implements Truthed {
    //@NotNull private final List<Truth> t;
    @Nullable
    public final List<Task> e;
    private final float confMin;

    float freq, conf; //running product

    public DynTruth(Op o, float confMin, List<Task> e) {
        if (o != CONJ)
            throw new UnsupportedOperationException("aggregate truth for " + o + " not implemented or not applicable");
        this.confMin = confMin;
        //this.t = t;
        this.e = e;
        freq = conf = 1f;
    }

    @NotNull
    public Budget budget() {
        //RawBudget b = new RawBudget();
        int s = e.size();
        assert (s > 0);
        float f = 1f / s;
//            for (Task x : e) {
//                BudgetMerge.plusBlend.apply(b, x.budget(), f);
//            }
//            return b;
        return BudgetFunctions.fund(e, f);
    }

    @Nullable
    public long[] evidence() {
        return e == null ? null : Stamp.zip(e);
    }

    @Override
    @Nullable
    public Truth truth() {
        return conf <= 0 ? null : $.t(freq, conf);
    }

    public boolean add(@Nullable Truth truth) {
        if (truth == null)
            return false;

        //specific to Truth.Intersection:
        conf *= truth.conf();
        if (conf < confMin)
            return false;
        freq *= truth.freq();
        return true;
    }

    @Override
    public String toString() {
        return truth().toString();
    }

    public DynamicBeliefTask task(Compound template, boolean beliefOrGoal, long cre, long occ, Budget b) {

        DynamicBeliefTask t = new DynamicBeliefTask(template, beliefOrGoal ? Symbols.BELIEF : Symbols.GOAL,
                truth(), cre, occ, evidence());
        t.setBudget(
                b != null ? b : budget()
        );
        t.log("Dynamic");
        return t;

    }
}
