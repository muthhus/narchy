package nars.concept.dynamic;

import jcog.list.FasterList;
import jcog.pri.Priority;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.DefaultBeliefTable;
import nars.task.AnswerTask;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;


public class DynamicBeliefTable extends DefaultBeliefTable {

    private final DynamicConcept dynamicConcept;
    final DynamicTruthModel model;
    private final boolean beliefOrGoal;

    //static final boolean rejectDerivations = false;

    @Override
    public void add(@NotNull Task input, @NotNull TaskConcept concept, @NotNull NAR nar) {
        if (input instanceof AnswerTask) {
            return; //dont insert its own dynamic belief task, causing a feedback loop
        }

        super.add(input, concept, nar);
    }

    public DynamicBeliefTable(DynamicConcept dynamicConcept, DynamicTruthModel model, boolean beliefOrGoal) {
        super();
        this.dynamicConcept = dynamicConcept;
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
    }


    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when) {
        return generate(template, when, dynamicConcept.nar.time(), null);
    }


    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when, long now, @Nullable Priority b) {

        DynTruth yy = truth(when, template, true);
        if (yy == null)
            return null;

        //compute the optimistic temporal union of the component's occurrences
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        @Nullable FasterList<Task> ee = yy.e;
        for (int i = 0, e1Size = ee.size(); i < e1Size; i++) {
            Task x = ee.get(i);
            long s = x.start();
            long e = x.end();
            if (s < start) start = s;
            if (e > end) end = e;
        }

        return yy.task(template, beliefOrGoal, now, start, end, b, dynamicConcept.nar);
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
        return truth(when, (Compound)(dynamicConcept.term().dt(dt)), evidence);
    }

    @Nullable
    public DynTruth truth(long when, long now, @NotNull Compound template, boolean evidence) {
        return model.eval(template, beliefOrGoal, when, now, evidence, dynamicConcept.nar); //newDyn(evidence);
    }

    @Override
    public Task match(long when, long now, int dur, @Nullable Task target, Compound template, boolean noOverlap, Random rng) {

        if (template == null) {
            template =
                    dynamicConcept.nar.terms.retemporalize(target.term(),
                            target.isEternal() ?
                                    dynamicConcept.nar.terms.retemporalizationDTERNAL : dynamicConcept.nar.terms.retemporalizationZero); //TODO move this somewhere else where it can use the NAR's index
        }

        Task y = generate(template, when);

        Task x = super.match(when, now, dur, target, template, noOverlap, rng);

        if (x == null) return y;
        if (y == null || x.equals(y)) return x;

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
