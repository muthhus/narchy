package nars.nal.meta;

import com.gs.collections.api.set.MutableSet;
import nars.Global;
import nars.Op;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.meta.match.EllipsisTransform;
import nars.term.Compound;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

abstract public class PatternCompound extends GenericCompound {

    public final int sizeCached;
    public final int volCached;
    public final int structureCached;
    public final boolean imgCached;

    @NotNull
    public final Term[] termsCached;


    PatternCompound(@NotNull Compound seed, @NotNull TermContainer subterms) {
        super(seed.op(), seed.dt(), subterms);

        if (seed.isNormalized())
            this.setNormalized();

        sizeCached = seed.size();
        structureCached =
                //seed.structure() & ~(Op.VariableBits);
                seed.structure() & ~(Op.VAR_PATTERN.bit);
        this.volCached = seed.volume();
        this.termsCached = subterms.terms();
        this.imgCached = op.isImage();

    }


    abstract protected static class PatternCompoundWithEllipsis extends PatternCompound {

        @Nullable
        protected final Ellipsis ellipsis;

        PatternCompoundWithEllipsis(@NotNull Compound seed, @Nullable Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, subterms);

            this.ellipsis = ellipsis;
            if (ellipsis == null)
                throw new RuntimeException("no ellipsis");


        }

        abstract protected boolean matchEllipsis(@NotNull Compound y, @NotNull FindSubst subst);

        protected boolean canMatch(@NotNull Compound y) {
            int yStructure = y.structure();
            return ((yStructure | structureCached) == yStructure);
        }

        @Override
        public boolean match(@NotNull Compound y, @NotNull FindSubst subst) {
            return canMatch(y) && matchEllipsis(y, subst);
        }


    }


    public static class PatternCompoundWithEllipsisLinear extends PatternCompoundWithEllipsis {

        public PatternCompoundWithEllipsisLinear(@NotNull Compound seed, @Nullable Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
        }

        @Override
        protected boolean matchEllipsis(@NotNull Compound y, @NotNull FindSubst subst) {
            return subst.matchEllipsedLinear(this, ellipsis, y);
        }

    }


    /**
     * requies dt exact match, for example, when matching Images (but not temporal terms)
     */
    abstract public static class PatternCompoundWithEllipsisLinearDT extends PatternCompoundWithEllipsisLinear {

        public PatternCompoundWithEllipsisLinearDT(@NotNull Compound seed, @Nullable Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
        }

        @Override
        protected boolean canMatch(@NotNull Compound y) {
            return (dt == y.dt() && super.canMatch(y));
        }


    }

    public static final class PatternCompoundWithEllipsisLinearImage extends PatternCompoundWithEllipsisLinearDT {

        public PatternCompoundWithEllipsisLinearImage(@NotNull Compound seed, @Nullable Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
        }

        /**
         * if they are images, they must have same dt
         */
        @Override
        protected boolean matchEllipsis(@NotNull Compound y, @NotNull FindSubst subst) {
            return (subst.matchEllipsisWithImage(this, ellipsis, y) && super.matchEllipsis(y, subst));
        }

    }

    /**
     * does not compare specific image dt
     */
    public static final class PatternCompoundWithEllipsisLinearImageTransform extends PatternCompoundWithEllipsisLinear {

        public PatternCompoundWithEllipsisLinearImageTransform(@NotNull Compound seed, @Nullable Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
        }

        @Override
        protected boolean matchEllipsis(@NotNull Compound y, @NotNull FindSubst subst) {
            return subst.matchCompoundWithEllipsisTransform(this, (EllipsisTransform) ellipsis, y);
        }
    }

    public static final class PatternCompoundWithEllipsisCommutive extends PatternCompoundWithEllipsis {

        public PatternCompoundWithEllipsisCommutive(@NotNull Compound seed, @Nullable Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
        }

        /**
         * commutive compound match: Y into X which contains one ellipsis
         * <p>
         * X pattern contains:
         * <p>
         * one unmatched ellipsis (identified)
         * <p>                    //HACK should not need new list
         * <p>
         * zero or more "constant" (non-pattern var) terms
         * all of which Y must contain
         * <p>
         * zero or more (non-ellipsis) pattern variables,
         * each of which may be matched or not.
         * matched variables whose resolved values that Y must contain
         * unmatched variables determine the amount of permutations/combinations:
         * <p>
         * if the number of matches available to the ellipse is incompatible with the ellipse requirements, fail
         * <p>
         * (total eligible terms) Choose (total - #normal variables)
         * these are then matched in revertable frames.
         * <p>
         * *        proceed to collect the remaining zero or more terms as the ellipse's match using a predicate filter
         *
         * @param y the compound being matched to this
         */
        @Override
        protected boolean matchEllipsis(@NotNull Compound y, @NotNull FindSubst subst) {
            //return subst.matchEllipsedCommutative(
            //        this, ellipsis, y
            //);
            //public final boolean matchEllipsedCommutative(@NotNull Compound X, @NotNull Ellipsis Xellipsis, @NotNull Compound Y) {

            Set<Term> xFree = Global.newHashSet(0); //Global.newHashSet(0);

            //constant terms which have been verified existing in Y and will not need matched
            Set<Term> alreadyInY = Global.newHashSet(0);

            boolean ellipsisMatched = false;
            for (Term x : terms()) {

                //boolean xVar = x.op() == type;
                //ellipsis to be matched in stage 2
                if (x == ellipsis)
                    continue;

                Term v = subst.term(x); //xVar ? getXY(x) : x;

                if (v instanceof EllipsisMatch) {

                    //assume it's THE ellipsis here, ie. x == xEllipsis by testing that Y contains all of these
                    if (!((EllipsisMatch) v).addWhileMatching(y, alreadyInY, ellipsis.sizeMin())) {
                        return false;
                    } else {
                        //Xellipsis = null;
                        ellipsisMatched = true;
                        break; //continued below
                    }

                } else if (v != null) {

                    if (!y.containsTerm(v)) {
                        //required but not actually present in Y
                        return false;
                    } else {
                        alreadyInY.add(v);
                    }

                } else {

                    xFree.add(x);
                }


            }

            MutableSet<Term> yFree = y.toSet();

            if (ellipsisMatched) {
                //Xellipsis = null;
                return alreadyInY.equals(yFree);
            } else {

                yFree.removeAll(alreadyInY);

                int numRemainingForEllipsis = yFree.size() - xFree.size();
                if (ellipsis.validSize(numRemainingForEllipsis)) {

                    return subst.matchCommutiveRemaining(ellipsis, xFree, yFree);

                } else {
                    //wouldnt be enough remaining matches to satisfy ellipsis cardinality
                    return false;
                }

            }

        }

    }

    public static final class PatternCompoundSimple extends PatternCompound {

        public PatternCompoundSimple(@NotNull Compound seed, @NotNull TermContainer subterms) {
            super(seed, subterms);
        }

        @Override
        public boolean match(@NotNull Compound y, @NotNull FindSubst subst) {
            @NotNull TermVector subterms = this.subterms;
            if (canMatch(y)) {
                TermContainer ysubs = y.subterms();
                return ((y.isCommutative()) ?
                        subst.matchPermute(subterms, ysubs) :
                        subst.matchLinear(subterms, ysubs));
            }
            return false;
        }

        protected final boolean canMatch(@NotNull Compound y) {

            int yStructure = y.structure();

            return ((structureCached | yStructure) == yStructure) &&
                    (sizeCached == y.size()) &&
                    (volCached <= y.volume()) &&
                    (!imgCached || /*image &&*/ (dt == y.dt()));
        }


    }


//    PatternCompound(@NotNull Compound seed) {
//        this(seed, (TermVector) seed.subterms());
//    }


    @NotNull
    @Override
    public Term[] terms() {
        return termsCached;
    }

    @Override
    public final int structure() {
        return structureCached;
    }

    @Override
    abstract public boolean match(@NotNull Compound y, @NotNull FindSubst subst);
    //abstract protected boolean canMatch(@NotNull Compound y);


}
/**
 * Created by me on 12/26/15.
 */ //    public static class VariableDependencies extends DirectedAcyclicGraph<Term,String> {
//
//
//        public Op type;
//
//        /* primary ==> secondary */
//        protected void dependency(Term primary, Term secondary) {
//            addVertex(primary);
//            addVertex(secondary);
//            try {
//                addDagEdge(primary, secondary, "d" + edgeSet().size()+1);
//            } catch (CycleFoundException e) {
//                //System.err.println(e);
//            }
//        }
//
//
//        public static class PatternVariableIndex extends VarPattern {
//
//            public final Compound parent;
//            public final int index;
//
//            public PatternVariableIndex(String id, Compound parent, int index) {
//                super(id);
//                this.parent = parent;
//                this.index = index; //first index
//            }
//            public PatternVariableIndex(Variable v, Compound parent) {
//                this(v.id, parent, parent.indexOf(v));
//            }
//
//            public String toString() {
//                return super.toString() + " @ " + parent + " index " + index;
//            }
//        }
//
//        public static class RematchedPatternVariableIndex extends PatternVariableIndex {
//
//            public RematchedPatternVariableIndex(PatternVariableIndex i) {
//                super(i.id + "_", i.parent, i.index);
//            }
//        }
//
//
//        final Map<Variable,PatternVariableIndex> variables = Global.newHashMap();
//
//        public VariableDependencies(Compound c, Op varType) {
//            super(null);
//
//            this.type = varType;
//
//            c.recurseTerms( (s, p) -> {
//                boolean existed = !addVertex(s);
//
//                if (p == null)
//                    return; //top level
//
//                addVertex(p);
//
//                //if (t instanceof Compound) {
//
//                //compoundIn.put((Compound)p, (Compound)t);
//
//                if (s.op(varType)) {
//                    if (existed) {
//                        PatternVariableIndex s0 = variables.get(s);
//                        s = new RematchedPatternVariableIndex(s0); //shadow variable dependent
//                        //dependency(s0, s); //variable re-use after first match
//
//                        //compound depends on existing variable
//                        dependency(s0, p);
//                        dependency(p, s);
//                    } else {
//                        //variable depends on existing compound
//                        PatternVariableIndex ss = new PatternVariableIndex((Variable) s, (Compound) p);
//                        variables.put((Variable) s, ss);
//                        dependency(p, ss);
//                    }
//                }
//                else {
//                    if (s.isCommutative()) {
//                        //term is commutive
//                        //delay commutive terms to the 2nd stage
//                        dependency(Op.Imdex, s);
//                    } else {
//
//                        //term depends on existing compound
//                        dependency(p, s);
//                    }
//                }
////                }
////                else {
////                    if (!t.op(varType)) return;
////
////                    varIn.put((Variable) t, (Compound) p);
////                    compHas.put((Compound)p, (Variable)t);
////
////                    try {
////                        addDagEdge(p, t,  "requries(" + t + "," + p + ")");
////                    } catch (Exception e1) {
////                        System.err.println(e1);
////                    }
////                }
//            });
//
//            Term last = null;
//            //DepthFirstIterator ii = new DepthFirstIterator(this, c);
//            Iterator ii = iterator(); //topological
//            while (ii.hasNext()) last = (Term) ii.next();
//
//            //second stage as a shadow node
//            dependency(last, Op.Imdex);
//
//
//
//        }
//    }
//
