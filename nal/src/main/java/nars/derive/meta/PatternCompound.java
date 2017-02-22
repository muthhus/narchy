package nars.derive.meta;

import nars.$;
import nars.Op;
import nars.derive.meta.match.Ellipsis;
import nars.derive.meta.match.EllipsisMatch;
import nars.derive.meta.match.EllipsisTransform;
import nars.derive.meta.match.ImageMatch;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.mutate.Choose1;
import nars.term.mutate.Choose2;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

abstract public class PatternCompound extends GenericCompound {

    public final int sizeCached;
    public final int structureCached;

    PatternCompound(@NotNull Compound seed, @NotNull TermContainer subterms) {
        super(seed.op(), seed.dt(), subterms);

        if (seed.isNormalized())
            this.setNormalized();

        sizeCached = seed.size();
        structureCached =
                //seed.structure() & ~(Op.VariableBits);
                seed.structure() & ~(Op.VAR_PATTERN.bit);
    }


    @Override
    public final int structure() {
        return structureCached;
    }


    abstract protected static class PatternCompoundWithEllipsis extends PatternCompound {

        @NotNull
        protected final Ellipsis ellipsis;

        PatternCompoundWithEllipsis(@NotNull Compound seed, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, subterms);

            this.ellipsis = ellipsis;

        }

        abstract protected boolean matchEllipsis(@NotNull Compound y, @NotNull Unify subst);

        @Override
        public final boolean unify(@NotNull Term y, @NotNull Unify subst) {
            return op == y.op() && y.hasAll(structureCached) && matchEllipsis((Compound)y, subst);
        }


    }


    public static class PatternCompoundWithEllipsisLinear extends PatternCompoundWithEllipsis {

        public PatternCompoundWithEllipsisLinear(@NotNull Compound seed, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
        }

        @Override
        protected boolean matchEllipsis(@NotNull Compound y, @NotNull Unify subst) {
            return matchEllipsedLinear(y, subst);
        }

        /**
         * non-commutive compound match
         * X will contain at least one ellipsis
         * <p>
         * match subterms in sequence
         * <p>
         * WARNING this implementation only works if there is one ellipse in the subterms
         * this is not tested for either
         */
        protected final boolean matchEllipsedLinear(@NotNull Compound Y, @NotNull Unify subst) {

            int i = 0, j = 0;
            int xsize = sizeCached;
            int ysize = Y.size();

            //TODO check for shim and subtract xsize?

            while (i < xsize) {
                Term x = term(i++);

                if (x instanceof Ellipsis) {
                    int available = ysize - j;

                    Term eMatched = subst.xy(x); //EllipsisMatch, or null
                    if (eMatched == null) {

                        //COLLECT
                        if (i == xsize) {
                            //SUFFIX
                            if (!ellipsis.validSize(available))
                                return false;

                            return subst.putXY(ellipsis, EllipsisMatch.match(Y, j, j + available));

                        } else {
                            //PREFIX the ellipsis occurred at the start and there are additional terms following it
                            //TODO
                            return false;
                        }
                    } else {
                        //previous match exists, match against what it had
//                        if (i == xsize) {
////                        //SUFFIX - match the remaining terms against what the ellipsis previously collected
////                        //HACK this only works with EllipsisMatch type
////                        Term[] sp = ((EllipsisMatch) eMatched).term;
////                        if (sp.length!=available)
////                            return false; //incorrect size
////
////                        //match every item
////                        for (Term aSp : sp) {
////                            if (!match(aSp, Y.term(j++)))
////                                return false;
////                        }
//                        } else {
//                            //TODO other cases
//                            return false;
//                        }

                    }
                } else {
                    if (ysize <= j || !subst.unify(x, Y.term(j++)))
                        return false;
                }
            }

            return true;
        }


    }


    /**
     * requies dt exact match, for example, when matching Images (but not temporal terms)
     */
    abstract public static class PatternCompoundWithEllipsisLinearDT extends PatternCompoundWithEllipsisLinear {

        public PatternCompoundWithEllipsisLinearDT(@NotNull Compound seed, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
        }

//        @Override
//        protected boolean canMatch(@NotNull Compound y) {
//            return (dt == y.dt() && super.canMatch(y));
//        }


    }

    public static final class PatternCompoundWithEllipsisLinearImage extends PatternCompoundWithEllipsisLinearDT {

        //private final int ellipseIndex;

        public PatternCompoundWithEllipsisLinearImage(@NotNull Compound seed, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
            //this.ellipseIndex = indexOf(ellipsis);
        }

//        @Override
//        protected boolean canMatch(@NotNull Compound y) {
//            return super.canMatch(y);
//        }

//        @Override
//        protected boolean matchEllipsis(@NotNull Compound y, @NotNull Unify subst) {
//            return matchEllipsisWithImage(y) && super.matchEllipsis(y, subst);
//        }

//        public boolean matchEllipsisWithImage(@NotNull Compound y) {
//
//            int xdt = dt();
//
//            //compare relation from beginning as in non-ellipsis case
//            //OR compare relation from end
//            return (ellipseIndex >= xdt) ? (xdt == y.dt()) : ((sizeCached - xdt) == (y.size() - y.dt()));
//
//        }


    }

    /**
     * does not compare specific image dt
     */
    public static final class PatternCompoundWithEllipsisLinearImageTransform extends PatternCompoundWithEllipsisLinear {

        @NotNull
        private final EllipsisTransform ellipsisTransform;

        public PatternCompoundWithEllipsisLinearImageTransform(@NotNull Compound seed, @NotNull EllipsisTransform ellipsis, @NotNull TermContainer subterms) {
            super(seed, ellipsis, subterms);
            this.ellipsisTransform = ellipsis;
        }

        @Override
        protected boolean matchEllipsis(@NotNull Compound y, @NotNull Unify subst) {

            //return subst.matchCompoundWithEllipsisTransform(this, (EllipsisTransform) ellipsis, y);
            //public boolean matchCompoundWithEllipsisTransform(@NotNull Compound X, @NotNull EllipsisTransform et, @NotNull Compound Y) {

            EllipsisTransform et = this.ellipsisTransform;
            @NotNull Term from = et.from;
            if (from.equals(Op.Imdex)) {
                Term n = subst.resolve(et.to);
                if (n != null /*&& !n.equals(y)*/) {

                    //the indicated term should be inserted
                    //at the index location of the image
                    //being processed. (this is the opposite
                    //of the other condition of this if { })

                    return matchEllipsedLinear(y, subst) &&
                            subst.replaceXY(et,
                                    ImageMatch.put(subst.xy(et), n, y));

                }
            } else {
                Term n = subst.resolve(from);
//                if (n == null) {
//                    //select at random TODO make termutator
//                    int imageIndex = random.nextInt(Y.size());
//                    return (putXY(et.from, Y.term(imageIndex)) && matchEllipsedLinear(X, e, Y)) &&
//                            replaceXY(e, ImageMatch.take(term(e), imageIndex));
//                }

                if (n != null /*&& n.op() != subst.type*/) {
                    int imageIndex = y.indexOf(n);
                    if (imageIndex != -1)
                        return matchEllipsedLinear(y, subst) &&
                                subst.replaceXY(et,
                                        ImageMatch.take(subst.xy(et), imageIndex));
                }
            }
            return false;
        }

    }

    public static final class PatternCompoundWithEllipsisCommutive extends PatternCompoundWithEllipsis {

        public PatternCompoundWithEllipsisCommutive(@NotNull Compound seed, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
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
        protected boolean matchEllipsis(@NotNull Compound y, @NotNull Unify subst) {
            //return subst.matchEllipsedCommutative(
            //        this, ellipsis, y
            //);
            //public final boolean matchEllipsedCommutative(@NotNull Compound X, @NotNull Ellipsis Xellipsis, @NotNull Compound Y) {

            Set<Term> xFree = $.newHashSet(0); //Global.newHashSet(0);

            //constant terms which have been verified existing in Y and will not need matched
            Set<Term> alreadyInY = $.newHashSet(0);

            final Ellipsis ellipsis = this.ellipsis;

            boolean ellipsisMatched = false;

            int s = size();
            for (int i = 0; i < s; i++) {
                Term x = term(i);

                //boolean xVar = x.op() == type;
                //ellipsis to be matched in stage 2
                if (Objects.equals(x, ellipsis))
                    continue;

                Term v = subst.xy(x); //xVar ? getXY(x) : x;

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

                    if (y.containsTerm(v)) {
                        alreadyInY.add(v);
                    } else {
                        //required but not actually present in Y
                        return false;
                    }

                } else {

                    xFree.add(x);
                }


            }

            Set<Term> yFree = y.toSet();

            if (ellipsisMatched) {
                //Xellipsis = null;
                return alreadyInY.equals(yFree);
            } else {

                yFree.removeIf(alreadyInY::contains);

                int numRemainingForEllipsis = yFree.size() - xFree.size();

                //if not invalid size there wouldnt be enough remaining matches to satisfy ellipsis cardinality
                return ellipsis.validSize(numRemainingForEllipsis) &&
                        matchEllipsisCommutive(subst, xFree, yFree);


            }

        }

        /**
         * toMatch matched into some or all of Y's terms
         */
        boolean matchEllipsisCommutive(@NotNull Unify subst, @NotNull Set<Term> xFree, @NotNull Set<Term> yFree) {
            int xs = xFree.size();

            switch (xs) {
                case 0:
                    //match everything
                    return subst.putXY(ellipsis, EllipsisMatch.match(yFree));
                case 1:
                    Term theFreeX = xFree.iterator().next();
                    return yFree.size() == 1 ? subst.putXY(theFreeX, yFree.iterator().next()) : subst.addTermutator(
                            new Choose1(ellipsis, theFreeX, yFree));
                case 2:
                    return subst.addTermutator(
                            new Choose2(subst, ellipsis, xFree, yFree));
                default:
                    //3 or more combination
                    throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
            }

        }
    }

    public static final class PatternCompoundSimple extends PatternCompound {

//        private final int subStructureCached;
//        private final boolean commutative;

        public PatternCompoundSimple(@NotNull Compound seed, @NotNull TermContainer subterms) {
            super(seed, subterms);
//            this.commutative = Compound.commutative(op(), size());
//            this.subStructureCached = subterms().structure();
        }

//        @Override
//        public boolean unify(@NotNull Term y, @NotNull FindSubst subst) {
//
//            //since the compound op will already have been determined equal prior to calling this method,
//            // compare the subterms structure (without the compound superterm's bit contribution)
//            // because this will be more specific in cases where the bits are already set
//
//            TermContainer ysubs = y.subterms();
//
//            return ysubs.hasAll(sizeCached, subStructureCached, volCached) &&
//                    (
//                        commutative ?
//                            (subst.matchPermute(subterms, ysubs))
//                            :
//                            ((!imgCached || (dt == y.dt())) && subst.matchLinear(subterms, ysubs))
//                    );
//        }


    }



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
