package nars.concept.dynamic;

import jcog.Util;
import jcog.pri.Priority;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.DefaultBeliefTable;
import nars.task.AnswerTask;
import nars.term.Compound;
import nars.truth.Stamp;
import nars.truth.Truth;
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
    public Task add(@NotNull Task input, @NotNull TaskConcept concept, @NotNull NAR nar) {
        if (input instanceof AnswerTask) {
            return input; //dont insert its own dynamic belief task, causing a feedback loop
        }

        return super.add(input, concept, nar);
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

//    @Nullable
//    public DynamicBeliefTask generate(@NotNull Compound template, long when, long now) {
//        return generate(template, when, now, null);
//    }

    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when, long now, @Nullable Priority b) {

        DynTruth yy = truth(when, template, true);
        return yy != null ? yy.task(template, beliefOrGoal, now, when, b, dynamicConcept.nar) : null;
    }

    @Override
    @Nullable
    public Truth truth(long when, long now, int dur) {
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
    public @Nullable Task match(long when, long now, int dur, @Nullable Task target, Compound template, boolean noOverlap) {
        if (template == null) {
            Compound dct = dynamicConcept.term();
            template = dynamicConcept.nar.concepts.retemporalize(dct); //TODO move this somewhere else where it can use the NAR's index
        }

//        Compound template =
//                //use the provided target task as a temporal template if it matches with this
//                ((target != null) && Terms.equal(localTerm, target.term(), false, false, false)) ?
//                        target.term()
//                    :
//                localTerm;

        Task y = generate(template, when);

        Task x;
        //try {
            x = super.match(when, now, dur, target, template, noOverlap);
//        } catch (InvalidTaskException e) {
//            if (Param.DEBUG_EXTRA) {
//                System.err.println(e);
//            }
//            x = null;
//        }

        if (x == null) return y;
        if (y == null) return x;

//        //choose the non-overlapping one
//        if (noOverlap && target != null) {
//            if (Stamp.overlapping(y, target))
//                return x;
//            if (Stamp.overlapping(x, target))
//                return y;
//        }

        //choose higher confidence
        float xc = x.evi(when, dur);
        float yc = y.evi(when, dur);
        //if (!Util.equals(xc, yc, TRUTH_EPSILON)) {
            return xc >= yc ? x : y;
        //}
//
//        //choose based on originality (includes cyclic), but by default prefer the existing task not the dynamic one
//        return (x.originality() >= y.originality()) ? x : y;
    }
}
