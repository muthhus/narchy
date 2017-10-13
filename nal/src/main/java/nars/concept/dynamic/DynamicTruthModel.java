package nars.concept.dynamic;

import jcog.Util;
import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {


    @Nullable
    public DynTruth eval(Term superterm, boolean beliefOrGoal, long start, long end, boolean stamp, NAR n) {

        int sdt = superterm.dt();
        if (sdt == XTERNAL) {
            throw new RuntimeException("XTERNAL should not happen here");
        }

        Term[] inputs = components(superterm);
        assert (inputs.length > 0) : this + " yielded no dynamic components for superterm " + superterm;

        DynTruth d = new DynTruth(stamp ? new FasterList(inputs.length) : null);
        d.freq = d.conf = 1f;

        final float confMin = 0; //n.confMin.floatValue();

        int dur = n.dur();

        for (int i = 0; i < inputs.length; i++) {
            @NotNull Term subterm = inputs[i];
            Term actualSubterm = subterm; //in case unnegated

            boolean negated = subterm.op() == Op.NEG;
            if (negated)
                subterm = subterm.unneg();

            Concept subConcept = n.concept(subterm);
            if (subConcept == null)
                return null; //ok just missing
            else if (!(subConcept instanceof BaseConcept))
                throw new RuntimeException("dynamically evaluated term should have only believable subterms");

            int dt;
            if (superterm.op() == CONJ) {
                dt = superterm.subtermTimeSafe(actualSubterm);
//                if (dt == DTERNAL) {
//                    if (sdt != DTERNAL && sdt != XTERNAL) {
//                        return null; //dt = 0; //TODO maybe this should never happen, and if it does there is an error
//                    }
//                }
            } else {
                dt = DTERNAL; //
            }

            boolean evi = d.e != null;

            //TODO check these times
            long subStart, subEnd;
            if (start == ETERNAL) {
                subStart = subEnd = ETERNAL;
            } else {
                if (dt == DTERNAL) {
                    subStart = subEnd = start;
                } else {
                    subStart = start + dt;
                    subEnd = end + dt + subterm.dtRange();
                }
            }

            Task bt;
            Truth nt;
            if (evi) {
                //task
                bt = ((BeliefTable) ((BaseConcept) subConcept).table(beliefOrGoal ? BELIEF : GOAL))
                        .match(subStart, subEnd, subterm, n);
                if (bt == null) {
                    return null;
                }

                nt = bt.truth(subStart, subEnd, dur, 0f); //project to target time if task isnt at it
                if (nt == null)
                    return null;

            } else {
                //truth only
                bt = null;
                nt = n.truth(subConcept, beliefOrGoal ? BELIEF : GOAL, subStart, subEnd);
            }

            if (nt == null) {
                return null;
            }

            if (!add(i, d, nt.negIf(negated), confMin))
                return null;

            if (bt != null)
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
            assert (comp.length > 1);
        }

        @Override
        protected float f(float freq) {
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

        public Intersection(@NotNull Term... comp) {
            this.comp = comp;
            assert (comp.length > 1);
        }

        @Override
        protected final boolean add(int subterm, DynTruth d, Truth truth, float confMin) {

            float f0 = d.freq;
            d.freq *= f(truth.freq());

            //HACK for subterms beyond 2, if the frequency has not changed, do not decrease confidence
            if (subterm < 2 || !Util.equals(f0, d.freq, Param.TRUTH_EPSILON)) {
                d.conf *= truth.conf();
                if (d.conf < confMin)
                    return false;
            }


            return true;

        }

        protected float f(float freq) {
            return freq;
        }


        @NotNull
        @Override
        public Term[] components(Term superterm) {
            return comp;
        }

    }

    public static class Difference extends DynamicTruthModel {
        private final Term[] components;

        public Difference(Term[] xy) {
            super();

//            assert (!(xy[0] instanceof Bool) && !(xy[1] instanceof Bool));
//            assert (!(xy[0] instanceof Variable) && !(xy[1] instanceof Variable)) :
//                    xy[0] + " or " + xy[1] + " is a variable";

            this.components = xy;
        }

        public Difference(Term x, Term y) {
            this(new Term[]{x, y});
        }

        @NotNull
        @Override
        public Term[] components(Term superterm) {
            return components;
        }

        @Override
        protected boolean add(int subterm, DynTruth d, Truth t, float confMin) {
            float c = t.conf();
            if (subterm == 0) {
                if (c < confMin)
                    return false;
                d.conf = c;
                d.freq = t.freq();
            } else {
                d.conf *= c;
                if (d.conf < confMin)
                    return false;
                d.freq *= (1f - t.freq());

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

//    /**
//     * N-ary intersection truth function of subterms
//     */
//    public static class DynamicIntersection extends DynamicTruthModel.Intersection {
//        private final Term[] subterms;
//
//        public DynamicIntersection(Term term) {
//            this.subterms = term.subterms().theArray();
//            assert(subterms.length > 1);
//        }
//
//        @NotNull
//        @Override
//        public Term[] components(Term superterm) {
//            return subterms;
//        }
//    }

}