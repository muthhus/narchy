package nars.concept.dynamic;

import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {


    @Nullable
    public DynTruth eval(Term superterm, boolean beliefOrGoal, long start, long end, boolean stamp, NAR n) {

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

            Concept subConcept = n.concept(subterm);
            if (subConcept == null)
                return null; //ok just missing
            else if (!(subConcept instanceof BaseConcept))
                throw new RuntimeException("dynamically evaluated term should have only believable subterms");

            int dt = superterm.subtermTimeSafe(subterm);
            if (dt == DTERNAL)
                dt = 0; //TODO maybe this should never happen, and if it does there is an error

            boolean evi = d.e != null;

            Truth nt;
            Task bt = null;

            //TODO check these times
            long subStart, subEnd;
            if (start == ETERNAL) {
                subStart = subEnd = ETERNAL;
            } else {
                subStart = start + dt;
                subEnd = end + dt + subterm.dtRange();
            }

            if (evi) {
                //task
                bt = ((BeliefTable) ((BaseConcept) subConcept).table(beliefOrGoal ? BELIEF : GOAL))
                        .match(subStart, subEnd, subterm, false, n);
                if (bt == null) {
                    return null;
                }

                nt = bt.truth(subStart, subEnd, dur, 0f); //project to target time if task isnt at it
                if (nt == null)
                    return null;

            } else {
                //truth only
                nt = n.truth(subConcept, beliefOrGoal ? BELIEF : GOAL, subStart, subEnd);
            }

            if (nt == null || nt.conf() < n.confMin.floatValue() || !add(i, d, nt.negIf(negated), confMin)) {
                return null;
            }

            if (evi)
                d.e.add(bt);

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

    abstract public @NotNull Term[] components(Term superterm);

    //protected abstract DynTruth eval(Compound template, long when, boolean stamp, NAR n);

    protected abstract boolean add(int subterm, DynTruth d, Truth t, float confMin);


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
        public Term[] components(Term superterm) {
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
        public Term[] components(Term superterm) {
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
        public Term[] components(Term superterm) {
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

    /**
     * N-ary intersection truth function of subterms
     */
    public static class DynamicIntersection extends DynamicTruthModel.Intersection {
        private final Term[] subterms;

        public DynamicIntersection(Term term) {
            this.subterms = term.subterms().toArray();
        }

        @NotNull
        @Override
        public Term[] components(Term superterm) {
            return subterms;
        }
    }

}