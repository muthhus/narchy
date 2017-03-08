package nars.concept.dynamic;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.budget.Budget;
import nars.concept.CompoundConcept;
import nars.table.DefaultBeliefTable;
import nars.table.QuestionTable;
import nars.term.Compound;
import nars.term.Terms;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Param.TRUTH_EPSILON;

/**
 * Created by me on 12/4/16.
 */
public class DynamicBeliefTable extends DefaultBeliefTable {

    private final DynamicConcept dynamicConcept;
    final DynamicTruthModel model;
    private final boolean beliefOrGoal;

    //static final boolean rejectDerivations = false;


    @Override
    public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {
        if (input instanceof DynamicBeliefTask)
            return TruthDelta.zero; //dont insert its own dynamic belief task, causing a feedback loop
        else
            return super.add(input, questions, concept, nar);
    }

    public DynamicBeliefTable(DynamicConcept dynamicConcept, DynamicTruthModel model, boolean beliefOrGoal) {
        super();
        this.dynamicConcept = dynamicConcept;
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
    }

//    @Override
//    public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {
//        if (rejectDerivations && !input.isInput())
//            return null;
//        return super.add(input, questions, concept, nar);
//    }

    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when) {
        return generate(template, when, dynamicConcept.nar.time(), null);
    }

    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when, long now) {
        return generate(template, when, now, null);
    }

    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when, long now, @Nullable Budget b) {
        DynTruth yy = truth(when, template, true);
        return yy != null ? yy.task(template, beliefOrGoal, now, when, b) : null;
    }

    @Override
    @Nullable
    public Truth truth(long when, long now, float dur) {
        DynTruth d = dyntruth(when, now, false);
        return Truth.maxConf(d != null ? d.truth() : null, super.truth(when, now, dur) /* includes only non-dynamic beliefs */);
    }

    @Nullable
    DynTruth dyntruth(long when, long now, boolean evidence) {
        return truth(when, now, dynamicConcept.term(), evidence);
    }

    @Nullable
    public DynTruth truth(long when, @NotNull Compound template, boolean evidence) {
        return truth(when, when, template,  /*nar.concept(template)*/
                evidence);
    }

    @Nullable
    public DynTruth truth(long when, int dt, boolean evidence) {
        return truth(when, (Compound) $.terms.the(dynamicConcept.term(), dt), evidence);
    }

    @Nullable
    public DynTruth truth(long when, long now, Compound template, boolean evidence) {

//        if (templateConcept == null)
//            return null;


        return model.eval(template, beliefOrGoal, when, now, evidence, dynamicConcept.nar); //newDyn(evidence);
//            } else {
//                @NotNull BeliefTable table = beliefOrGoal ? templateConcept.beliefs() : templateConcept.goals();
//                if (table instanceof DynamicBeliefTable) {
//                    return ((DynamicBeliefTable)table).dyntruth(when, now, evidence);
//                } else {
//                    Task x = table.match(when, now);
//                    if (x == null)
//                        return null;
//                    else {
//                        DynTruth d = newDyn(evidence);
//                        if (d.add(x.truth().negated(negated))) {
//                            if (d.e != null)
//                                d.e.add(x);
//                        }
//                        return d;
//                    }
//                }
//            }

    }

//    @NotNull
//    private DynTruth newDyn(boolean evidence) {
//        final List<Task> e = evidence ? $.newArrayList(size()) : null;
//        return new DynTruth(dynamicConcept.op(), dynamicConcept.nar.confMin.floatValue(), e);
//    }

    @Override
    public @Nullable Task match(long when, long now, float dur, @Nullable Task target, boolean noOverlap) {
        Compound localTerm = $.terms.retemporalize( dynamicConcept.term() );

        Compound template =
                //use the provided target task as a temporal template if it matches with this
                ((target != null) && Terms.equal(localTerm, target.term(), false, false, false)) ?
                        target.term()
                    :
                localTerm;

        Task y = generate(template, when);

        Task x = super.match(when, now, dur, target, noOverlap);

        if (x == null) return y;
        if (y == null) return x;

        //choose the non-overlapping one
        if (noOverlap && target != null) {
            if (Stamp.overlapping(x, target))
                return y;
            if (Stamp.overlapping(y, target))
                return x;
        }

        //choose higher confidence
        float xc = x.conf(when, dur);
        float yc = y.conf(when, dur);
        if (!Util.equals(xc, yc, TRUTH_EPSILON)) {
            return xc > yc ? x : y;
        }

        //choose based on originality (includes cyclic), but by default prefer the existing task not the dynamic one
        return (x.originality() > y.originality()) ? x : y;
    }
}
