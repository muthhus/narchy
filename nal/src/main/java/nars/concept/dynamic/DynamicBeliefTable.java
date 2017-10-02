package nars.concept.dynamic;

import jcog.list.FasterList;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DynamicBeliefTable extends DefaultBeliefTable {

    final DynamicTruthModel model;
    private final boolean beliefOrGoal;
    private final Term term;


    public DynamicBeliefTable(Term c, TemporalBeliefTable t, DynamicTruthModel model, boolean beliefOrGoal) {
        super(t);
        this.term = c;
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
    }

    @Nullable
    public NALTask generate(@NotNull Term template, long start, long end, @Nullable Prioritized b, NAR nar) {

        DynTruth yy = truth(start, end, template, true, nar);
        if (yy == null)
            return null;

        //compute the optimistic temporal union of the component's occurrences
        @Nullable FasterList<Task> ee = yy.e;
        if (ee == null || ee.isEmpty())
            return null;

//        boolean ete = false;
//        long end = Long.MIN_VALUE;
//        long start = Long.MAX_VALUE;
//        for (int i = 0, e1Size = ee.size(); i < e1Size; i++) {
//            Task x = ee.get(i);
//            long s = x.start();
//            if (s!=ETERNAL) {
//                long e = x.end();
//                if (s < start) start = s;
//                if (e > end) end = e;
//            } else {
//                ete = true;
//            }
//        }
//        if (end == Long.MIN_VALUE) {
//            assert(ete); //should be eternal in this case
//            start = end = ETERNAL;
//        }


        return yy.task(template, beliefOrGoal, nar.time(), start, end, b, nar);
    }

    @Override
    public Truth truth(long start, long end, NAR nar) {
        DynTruth d = truth(start, end, term, false, nar);
        return Truth.maxConf(d != null ? d.truth() : null,
                super.truth(start, end, nar) /* includes only non-dynamic beliefs */);
    }


    @Nullable
    public DynTruth truth(long start, long end, @NotNull Term template, boolean evidence, NAR nar) {
        return model.eval(template, beliefOrGoal, start, end, evidence, nar); //newDyn(evidence);
    }

    @Override
    public Task match(long start, long end, @NotNull Term template, boolean noOverlap, NAR nar) {
        Task x = super.match(start, end, template, noOverlap, nar);

        Task y = generate(template, start, end, null, nar);
        if (y == null || y.equals(x)) return x;

        boolean dyn;
        if (x == null) {
            dyn = true;
        } else {
            //choose higher confidence
            int dur = nar.dur();
            float xc = x.evi(start, end, dur);
            float yc = y.evi(start, end, dur);

            //prefer the existing task within a small epsilon lower for efficiency
            dyn = yc >= xc + Param.TRUTH_EPSILON;
        }

        if (dyn) {
            Activate.activate(y, y.priElseZero(), nar);
            return y;
        } else {
            return x;
        }

    }
}
