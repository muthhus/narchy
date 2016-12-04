package nars.concept.dynamic;

import com.google.common.collect.Iterators;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.obj.Termject;
import nars.truth.DynTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static nars.time.Tense.DTERNAL;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {


    /**
     * N-ary intersection truth function of subterms
     */
    public static final DynamicTruthModel Intersection = new IntersectionTruth(null) {
        @NotNull @Override public Term[] components(Compound superterm) {
            return superterm.terms();
        }
    };

    @Nullable public DynTruth eval(Compound superterm, boolean beliefOrGoal, long when, long now, boolean stamp, NAR n) {

        Term[] inputs = components(superterm);

        DynTruth d = eval(superterm, when, stamp, n);

        float confMin = n.confMin.floatValue();

        for (int i = 0; i < inputs.length; i++) {
            Term subterm = inputs[i];

            boolean negated = subterm.op() == Op.NEG;
            if (negated)
                subterm = subterm.unneg();

            Concept subConcept = n.concept(subterm);
            if (subConcept != null) {
                BeliefTable table = beliefOrGoal ? subConcept.beliefs() : subConcept.goals();
                boolean tableDynamic = table instanceof DynamicBeliefTable;
                if (!tableDynamic && table.isEmpty())
                    return null;

                int dt = superterm.subtermTime(subterm);
                if (dt == DTERNAL) dt = 0;

                @Nullable Truth nt;
                if (tableDynamic) {
                    boolean evi = d.e != null;
                    @Nullable DynTruth ndt = ((DynamicBeliefTable) table).truth(when + dt, now, (Compound) subterm, evi);
                    if (ndt!=null) {
                        Truth ntt = ndt.truth();
                        if (ntt != null && add(d, ntt.negated(negated), confMin)) {
                            if (d.e != null) {
                                d.e.addAll(ndt.e);
                            }
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    nt = table.truth(when + dt, now);
                    if (nt != null && add(d, nt.negated(negated), confMin)) {
                        if (d.e != null) {
                            Task bt = table.match(when + dt, now);
                            if (bt != null) {
                                d.e.add(bt); //HACK this doesnt include the non-top tasks which may contribute to the evaluated truth during truthpolation
                            }
                        }
                    } else {
                        return null;
                    }
                }
                /*if (u!=null)
                    u = u.negated( t.op() == NEG );*/
            } else {
                return null;
            }
        }

//        //if (template instanceof Compound) {
        return d;
    }

    abstract public @NotNull Term[] components(Compound superterm);

    protected abstract DynTruth eval(Compound template, long when, boolean stamp, NAR n);

    protected abstract boolean add(DynTruth d, Truth t, float confMin);


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


    public static class IntersectionTruth extends DynamicTruthModel {

        private final Term[] comp;

        public IntersectionTruth(Term[] comp) {
            this.comp = comp;
        }

        @Override
        protected boolean add(DynTruth d, Truth truth, float confMin) {

            //specific to Truth.Intersection:
            d.conf *= truth.conf();
            if (d.conf < confMin)
                return false;

            d.freq *= truth.freq();

            return true;

        }


        @NotNull
        @Override
        public Term[] components(Compound superterm) {
            return comp;
        }

        @Override
        protected DynTruth eval(Compound template, long when, boolean stamp, NAR n) {

            DynTruth d = new DynTruth(stamp? $.newArrayList(0) : null);
            d.freq = d.conf = 1f;
            return d;

        }
    }
}
