package nars.concept.dynamic;

import nars.*;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.term.Compound;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

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

    @Nullable
    public Budget budget() {
        //RawBudget b = new RawBudget();
        int s = e.size();
        assert (s > 0);

        if (s > 1) {
            float f = 1f / s;
            //            for (Task x : e) {
            //                BudgetMerge.plusBlend.apply(b, x.budget(), f);
            //            }
            //            return b;
            return BudgetFunctions.fund(e, f, true);
        } else {
            return e.get(0).budget().clone();
        }
    }

    @Nullable
    public long[] evidence() {

        //return e == null ? null :
        return Stamp.zip(e);
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

    @Nullable public DynamicBeliefTask task(@NotNull Compound c, boolean beliefOrGoal, long cre, long start, @Nullable Budget b, NAR nar) {

        Budget budget = b != null ? b : budget();
        if (budget == null || budget.isDeleted())
            return null;

        Truth tr = truth();
        if (tr == null)
            return null;

        //HACK try to reconstruct the term because it may be invalid
        int dt = c.dt();
        if (c.op().temporal && dt == DTERNAL && start!=ETERNAL)
            dt = 0; //actually it is measured at the current time so make it parallel

        c = compoundOrNull(nar.concepts.the(c.op(), dt, c.terms()));
        if (c == null)
            return null;


        c = Task.content(c, nar);
        if (c == null) return null;

        // normalize it
        c = compoundOrNull(nar.concepts.normalize(c));
        if (c == null) return null;

        // then if the term is valid, see if it is valid for a task
        if (!Task.taskContentValid(c, beliefOrGoal ? BELIEF : GOAL, null, true)) {
            return null;
        }

        long dur = (start!=ETERNAL && c.op() == CONJ) ? c.dtRange() : 0;

        DynamicBeliefTask dyn = new DynamicBeliefTask(c, beliefOrGoal ? Op.BELIEF : Op.GOAL,
                tr, cre, start, start + dur, evidence());
        dyn.setBudget( budget );
        if (Param.DEBUG)
            dyn.log("Dynamic");

        return dyn;
    }
}
