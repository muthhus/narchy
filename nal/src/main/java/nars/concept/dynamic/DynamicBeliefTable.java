package nars.concept.dynamic;

import com.google.common.collect.Iterators;
import nars.*;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.nal.Stamp;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.obj.Termject;
import nars.truth.DynTruth;
import nars.truth.Truth;
import nars.util.Util;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static nars.Op.NEG;
import static nars.Param.TRUTH_EPSILON;
import static nars.time.Tense.DTERNAL;

/**
 * Created by me on 12/4/16.
 */
public class DynamicBeliefTable extends DefaultBeliefTable {

    private DynamicConcept dynamicConcept;
    final DynamicTruthModel model;
    private final boolean beliefOrGoal;

    public DynamicBeliefTable(DynamicConcept dynamicConcept, DynamicTruthModel model, boolean beliefOrGoal, int eCap, int tCap, NAR nar) {

        super(dynamicConcept.newEternalTable(eCap), dynamicConcept.newTemporalTable(tCap, nar));
        this.dynamicConcept = dynamicConcept;
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
    }

    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when) {
        return generate(template, when, null);
    }

    @Nullable
    public DynamicBeliefTask generate(@NotNull Compound template, long when, @Nullable Budget b) {
        DynTruth yy = truth(when, template, true);
        return yy != null ? yy.task(template, beliefOrGoal, dynamicConcept.nar.time(), when, b) : null;
    }

    @Override
    @Nullable
    public Truth truth(long when, long now) {
        DynTruth d = dyntruth(when, now, false);
        return Truth.maxConf(d != null ? d.truth() : null, super.truth(when, now) /* includes only non-dynamic beliefs */);
    }

    @Nullable
    protected DynTruth dyntruth(long when, long now, boolean evidence) {
        return truth(when, now, dynamicConcept.term(), dynamicConcept, false, evidence);
    }

    @Nullable
    public DynTruth truth(long when, @NotNull Compound template, boolean evidence) {
        return truth(when, when, template, dynamicConcept /*nar.concept(template)*/,
                template.op() == NEG, evidence);
    }


    @Nullable
    public DynTruth truth(long when, int dt, boolean evidence) {
        return truth(when, (Compound) $.terms.the(dynamicConcept.term(), dt), evidence);
    }

    @Nullable
    private DynTruth truth(long when, long now, Compound template, @Nullable Concept templateConcept, boolean negated, boolean evidence) {

//        if (templateConcept == null)
//            return null;

        //if (template instanceof Compound) {
        DynTruth d = newDyn(evidence);
        int s = template.size();
        for (int i = 0; i < s; i++) {
            if (!subTruth(template, template.term(i), when, now, negated, d))
                return null;
        }
        return d;
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

    @NotNull
    private DynTruth newDyn(boolean evidence) {
        int n = size();
        final List<Task> e = evidence ? $.newArrayList(n) : null;
        return new DynTruth(dynamicConcept.op(), dynamicConcept.nar.confMin.floatValue(), e);
    }

    /**
     * returns true if the subterm was evaluated successfully, false otherwise
     */
    private boolean subTruth(Compound superterm, @NotNull Term subterm, long when, long now, boolean neg, @NotNull DynTruth d) {

        Term ss = subterm; //original pre-unnegated subterm for dt relative calculation

        boolean negated = (subterm.op() == NEG) != neg;
        if (negated)
            subterm = subterm.unneg();

        if (subterm instanceof Compound) {
            Compound cs = (Compound) subterm;
            if (subterm.hasAny(Op.INT)) {

                Iterator<Term> unrolled = unrollInts(cs);
                if (unrolled != null) {
                    while (unrolled.hasNext()) {
                        Term next = unrolled.next();
                        if (!(next instanceof Compound) || !subTruth((Compound) next, superterm, when, now, d, ss, negated))
                            return false;
                    }

                    return true;
                }

            } else {
                return subTruth(cs, superterm, when, now, d, ss, negated);
            }
        }

        return false;
    }

    private boolean subTruth(@NotNull Compound next, Compound template, long when, long now, @NotNull DynTruth d, @NotNull Term ss, boolean negated) {
        Concept subConcept = dynamicConcept.nar.concept(next);
        if (subConcept == null)
            return false;

        BeliefTable table = beliefOrGoal ? subConcept.beliefs() : subConcept.goals();
        boolean tableDynamic = table instanceof DynamicBeliefTable;
        if (!tableDynamic && table.isEmpty()) {
            return false;
        }

        int dt = template != null ? template.subtermTime(ss) : 0;
        if (dt == DTERNAL) dt = 0;

        //System.out.println(ss + " "+ dt + " in " + template);


        @Nullable Truth nt = null;
        if (tableDynamic) {
            boolean evi = d.e != null;
            @Nullable DynTruth ndt = ((DynamicBeliefTable) table).truth(when + dt, now, next, subConcept, negated, evi);
            //already negated via the parameter
            if (ndt != null && d.add(ndt.truth())) {
                if (d.e != null) {
                    d.e.addAll(ndt.e);
                }
                return true;
            }
        } else {
            nt = table.truth(when + dt, now);
            if (nt != null && d.add(nt.negated(negated))) {
                if (d.e != null) {
                    Task bt = table.match(when + dt, now);
                    if (bt != null) {
                        d.e.add(bt); //HACK this doesnt include the non-top tasks which may contribute to the evaluated truth during truthpolation
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * unroll IntInterval's
     */
    private Iterator<Term> unrollInts(@NotNull Compound c) {
        if (!c.hasAny(Op.INT))
            return Iterators.singletonIterator(c); //no IntInterval's early exit

        Compound cc = c;

        Map<ByteList, Termject.IntInterval> intervals = new HashMap();
        c.pathsTo(x -> x instanceof Termject.IntInterval ? ((Termject.IntInterval) x) : null, (ByteList p, Termject.IntInterval x) -> {
            intervals.put(p.toImmutable(), x);
            return true;
        });

        switch (intervals.size()) {

            case 1: //1D
            {
                Map.Entry<ByteList, Termject.IntInterval> e = intervals.entrySet().iterator().next();
                Termject.IntInterval i1 = e.getValue();
                int max = i1.max();
                int min = i1.min();
                List<Term> t = $.newArrayList(1 + max - min);
                for (int i = min; i <= max; i++) {
                    @Nullable Term c1 = $.terms.transform(cc, e.getKey(), $.the(i));
                    if (c1 != null)
                        t.add(c1);
                }
                return t.iterator();
            }

            case 2: //2D
                Iterator<Map.Entry<ByteList, Termject.IntInterval>> ee = intervals.entrySet().iterator();
                Map.Entry<ByteList, Termject.IntInterval> e1 = ee.next();
                Map.Entry<ByteList, Termject.IntInterval> e2 = ee.next();
                Termject.IntInterval i1 = e1.getValue();
                Termject.IntInterval i2 = e2.getValue();
                int max1 = i1.max(), min1 = i1.min(), max2 = i2.max(), min2 = i2.min();
                List<Term> t = $.newArrayList((1 + max2 - min2) * (1 + max1 - min1));

                for (int i = min1; i <= max1; i++) {
                    for (int j = min2; j <= max2; j++) {
                        Term c1 = $.terms.transform(cc, e1.getKey(), $.the(i));
                        if (!(c1 instanceof Compound))
                            //throw new RuntimeException("how not transformed to compound");
                            continue;
                        Term c2 = $.terms.transform((Compound) c1, e2.getKey(), $.the(j));
                        if (!(c2 instanceof Compound))
                            //throw new RuntimeException("how not transformed to compound");
                            continue;
                        t.add(c2);
                    }
                }
                return t.iterator();

            default:
                //either there is none, or too many -- just use the term directly
                return null;

        }

    }

    @Override
    public @Nullable Task match(long when, long now, @Nullable Task target, boolean noOverlap) {
        Compound template = target != null ? target.term() : dynamicConcept.term();

        Task y = generate(template, when);

        Task x = super.match(when, now, target, noOverlap);

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
        float xc = x.conf();
        float yc = y.conf();
        if (!Util.equals(xc, yc, TRUTH_EPSILON)) {
            return xc > yc ? x : y;
        }

        //choose based on originality (includes cyclic), but by default prefer the existing task not the dynamic one
        return (x.originality() > y.originality()) ? x : y;
    }
}
