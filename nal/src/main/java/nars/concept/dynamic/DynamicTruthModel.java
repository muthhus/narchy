package nars.concept.dynamic;

import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.DTERNAL;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {


    /**
     * N-ary intersection truth function of subterms
     */
    public static final DynamicTruthModel Intersection = new Intersection() {
        @NotNull
        @Override
        public Term[] components(Compound superterm) {
            return superterm.toArray();
        }
    };

    @Nullable
    public DynTruth eval(Compound superterm, boolean beliefOrGoal, long when, long now, boolean stamp, NAR n) {

        Term[] inputs = components(superterm);

        DynTruth d = new DynTruth(stamp ? new FasterList(0) : null);
        d.freq = d.conf = 1f;

        final float confMin = 0; //n.confMin.floatValue();

        int dur = n.dur();

        for (int i = 0; i < inputs.length; i++) {
            @NotNull Term subterm = inputs[i];

            boolean negated = subterm.op() == Op.NEG;
            if (negated)
                subterm = subterm.unneg();

            if (!(subterm instanceof Compound))
                continue;

            TaskConcept subConcept = (TaskConcept) n.concept(subterm);
            if (subConcept == null)
                return null;

            int dt = superterm.subtermTime(subterm);
            if (dt == DTERNAL)
                dt = 0; //TODO maybe this should never happen, and if it does there is an error

            boolean evi = d.e != null;

            Truth nt;
            if (evi) {
                //task
                Task bt = ((BeliefTable)subConcept.table(beliefOrGoal ? BELIEF : GOAL)).match( when + dt, null, (Compound)subterm, false, n);
                if (bt == null) {
                    return null;
                }

                nt = bt.truth(when + dt, dur); //project to target time if task isnt at it
                if (nt==null)
                    return null;

                d.e.add(bt);

            } else {
                //truth only
                nt = n.truth(subConcept, beliefOrGoal ? BELIEF : GOAL, when + dt);
            }

            if (nt == null || !add(i, d, nt.negIf(negated), confMin)) {
                return null;
            }


        }


//        //if (template instanceof Compound) {
        return commit(d);
    }

    /**
     * override for postprocessing
     */
    protected DynTruth commit(DynTruth d) {
        return d;
    }

    abstract public @NotNull Term[] components(Compound superterm);

    //protected abstract DynTruth eval(Compound template, long when, boolean stamp, NAR n);

    protected abstract boolean add(int subterm, DynTruth d, Truth t, float confMin);


//    /**
//     * unroll IntInterval's
//     */
//    private Iterator<Term> unrollInts(@NotNull Compound c) {
//        if (!c.hasAny(Op.INT))
//            return Iterators.singletonIterator(c); //no IntInterval's early exit
//
//        Compound cc = c;
//
//        Map<ByteList, Termject.IntInterval> intervals = new HashMap();
//        c.pathsTo(x -> x instanceof Termject.IntInterval ? ((Termject.IntInterval) x) : null, (ByteList p, Termject.IntInterval x) -> {
//            intervals.put(p.toImmutable(), x);
//            return true;
//        });
//
//        switch (intervals.size()) {
//
//            case 1: //1D
//            {
//                Map.Entry<ByteList, Termject.IntInterval> e = intervals.entrySet().iterator().next();
//                Termject.IntInterval i1 = e.getValue();
//                int max = i1.max();
//                int min = i1.min();
//                List<Term> t = $.newArrayList(1 + max - min);
//                for (int i = min; i <= max; i++) {
//                    @Nullable Term c1 = $.terms.transform(cc, e.getKey(), $.the(i));
//                    if (c1 != null)
//                        t.add(c1);
//                }
//                return t.iterator();
//            }
//
//            case 2: //2D
//                Iterator<Map.Entry<ByteList, Termject.IntInterval>> ee = intervals.entrySet().iterator();
//                Map.Entry<ByteList, Termject.IntInterval> e1 = ee.next();
//                Map.Entry<ByteList, Termject.IntInterval> e2 = ee.next();
//                Termject.IntInterval i1 = e1.getValue();
//                Termject.IntInterval i2 = e2.getValue();
//                int max1 = i1.max(), min1 = i1.min(), max2 = i2.max(), min2 = i2.min();
//                List<Term> t = $.newArrayList((1 + max2 - min2) * (1 + max1 - min1));
//
//                for (int i = min1; i <= max1; i++) {
//                    for (int j = min2; j <= max2; j++) {
//                        Term c1 = $.terms.transform(cc, e1.getKey(), $.the(i));
//                        if (!(c1 instanceof Compound))
//                            //throw new RuntimeException("how not transformed to compound");
//                            continue;
//                        Term c2 = $.terms.transform((Compound) c1, e2.getKey(), $.the(j));
//                        if (!(c2 instanceof Compound))
//                            //throw new RuntimeException("how not transformed to compound");
//                            continue;
//                        t.add(c2);
//                    }
//                }
//                return t.iterator();
//
//            default:
//                //either there is none, or too many -- just use the term directly
//                return null;
//
//        }
//
//    }


    /**
     * conf is multiplied, freq is OR'd
     */
    public static class Union extends DynamicTruthModel.Intersection {

        public Union(@NotNull Term... comp) {
            super(comp);
        }

        @Override
        public float f(float freq) {
            return 1f - freq;
        }

        @Override
        protected DynTruth commit(DynTruth d) {
            d.freq = 1f - d.freq;
            return d;
        }
    }

    public static class Intersection extends DynamicTruthModel {

        @NotNull
        private final Term[] comp;

        protected Intersection() {
            this.comp = Term.EmptyArray;
        }

        public Intersection(@NotNull Term... comp) {
            this.comp = comp;
        }

        @Override
        protected final boolean add(int subterm, DynTruth d, Truth truth, float confMin) {

            //specific to Truth.Intersection:
            d.conf *= c(truth.conf());
            if (d.conf < confMin)
                return false;

            d.freq *= f(truth.freq());

            return true;

        }

        public float f(float freq) {
            return freq;
        }

        public float c(float conf) {
            return conf;
        }


        @NotNull
        @Override
        public Term[] components(Compound superterm) {
            return comp;
        }

    }

    public static class Difference extends DynamicTruthModel {
        private final Term[] components;
        private Truth tx, ty;

        public Difference(Term x, Term y) {
            super();
            this.components = new Term[]{x, y};
        }

        @NotNull
        @Override
        public Term[] components(Compound superterm) {
            return components;
        }

        @Override
        protected DynTruth commit(DynTruth d) {
            //intersection(a, b.negated(), minConf);
            d.freq = tx.freq() * (1f - ty.freq());
            d.conf = tx.conf() * ty.conf();
            return d;
        }

        @Override
        protected boolean add(int subterm, DynTruth d, Truth t, float confMin) {
            if (subterm == 0)
                tx = t;
            else {
                ty = t;
//                if (t.conf() * tx.conf() < confMin) //early termination check
//                    return false;
            }

            return true;
        }
    }

    public static class Identity extends DynamicTruthModel {

        @NotNull
        private final Term[] components;

        public Identity(@NotNull Compound proxy, @NotNull Compound base) {
            this.components = new Term[]{base};
        }

        @NotNull
        @Override
        public Term[] components(Compound superterm) {
            return components;
        }

        @Override
        protected boolean add(int subterm, @NotNull DynTruth d, @NotNull Truth t, float confMin) {
            float c = t.conf();
            if (c >= confMin) {
                d.conf = c;
                d.freq = t.freq();
                return true;
            }
            return false;
        }
    }
}