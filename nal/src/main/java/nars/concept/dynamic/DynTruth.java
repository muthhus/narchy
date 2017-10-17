package nars.concept.dynamic;

import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.transform.Retemporalize;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth implements Truthed {

    @Nullable
    public final FasterList<Task> e;
    public Truthed truth;

    public float freq;
    public float conf; //running product

    public DynTruth(FasterList<Task> e) {
        //this.t = t;
        this.e = e;
        this.truth = null;
    }

    public void setTruth(Truthed truth) {
        this.truth = truth;
    }

    @Nullable
    public float budget() {
        //RawBudget b = new RawBudget();
        if (e == null)
            return Float.NaN;

        int s = e.size();
        assert (s > 0);

        if (s > 1) {
            //float f = 1f / s;
            //            for (Task x : e) {
            //                BudgetMerge.plusBlend.apply(b, x.budget(), f);
            //            }
            //            return b;
            //return e.maxValue(Task::priElseZero); //use the maximum of their truths
            return e.meanValue(Task::priElseZero); //average value
        } else {
            return e.get(0).priElseZero();
        }
    }

    @Nullable
    public long[] evidence() {
        return Stamp.zip(e.array(Stamp[]::new), Param.STAMP_CAPACITY);
    }

    @Nullable
    public short[] cause() {
        return e != null ? Cause.zip(e.array(Task[]::new) /* HACK */) : ArrayUtils.EMPTY_SHORT_ARRAY;
    }

    @Override
    @Nullable
    public PreciseTruth truth() {
        return conf == conf && conf <= 0 ? null : new PreciseTruth(freq, conf);
    }


    @Override
    public String toString() {
        return truth().toString();
    }

    NALTask task(/*@NotNull*/ Term c, boolean beliefOrGoal, NAR nar) {

        Truth tr0 = truth();
        if (tr0 == null)
            return null;

        if (c.op() == NEG) {
            c = c.unneg();
            tr0 = tr0.neg();
        }

        Truth tr = tr0.ditherFreqConf(nar.truthResolution.floatValue(), nar.confMin.floatValue(), 1f);
        if (tr == null)
            return null;

        float priority = budget();
        if (priority != priority) //deleted
            return null;

        long[] se = Task.range(e);
        long start = se[0];
        long end = se[1];

        Retemporalize r = start == ETERNAL ? Retemporalize.retemporalizeXTERNALToDTERNAL : Retemporalize.retemporalizeXTERNALToZero;
        if (null == (c = c.temporalize(r)))
            return null;


        // then if the term is valid, see if it is valid for a task
        if (!Task.taskContentValid(c, beliefOrGoal ? BELIEF : GOAL, nar, true))
            return null;




        NALTask dyn = new NALTask(c, beliefOrGoal ? Op.BELIEF : Op.GOAL,
                tr, nar.time(), start, end /*+ dur*/, evidence());
        dyn.cause = cause();
        dyn.setPri(priority);

        if (Param.DEBUG)
            dyn.log("Dynamic");

        return dyn;
    }
}
