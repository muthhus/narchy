package nars.truth;

import nars.$;
import nars.Op;
import nars.Symbols;
import nars.Task;
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

    @Nullable public final List<Task> e;
    public Truthed truth;

    public float freq;
    public float conf; //running product

    public DynTruth(List<Task> e) {
        //this.t = t;
        this.e = e;
        this.truth = null;
    }

    public void setTruth(Truthed truth) {
        this.truth = truth;
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
