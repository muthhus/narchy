package nars.derive;

import nars.Op;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.derive.mutate.Choose1;
import nars.derive.mutate.Choose2;
import nars.op.mental.AliasConcept;
import nars.term.Compound;
import nars.term.GenericCompoundDT;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;
import java.util.TreeSet;

import static nars.Op.*;

abstract public class PatternCompound extends GenericCompoundDT {

    final int sizeCached;
    final int structureNecessary;
    private final boolean commutative; //cached
    transient final private Op op; //cached
    private final int minVolumeNecessary;
    private final int size;

    PatternCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {
        super(new GenericCompound(op, subterms), dt);

        this.op = op;
        sizeCached = subterms.size();
        structureNecessary =
                structure() &
                          ~(VAR_PATTERN.bit
                        | (subterms.hasAny(ATOM) ?
                                  (INH.bit | PROD.bit) : 0) // inh and prod (in case of any contained functors HACK)
                        );
        commutative = super.isCommutative();
        minVolumeNecessary = volume();
        size = size();
    }


    @Override
    public boolean isCommutative() {
        return commutative;
    }

//    @Override
//    public final int structure() {
//        return structureNecessary;
//    }

    /**
     * slightly modified from general compound unification
     */
    @Override
    public boolean unify(@NotNull Term y, @NotNull Unify subst) {

        if (
                y.hasAll(structureNecessary) &&
                op == y.op() &&
                size == y.size()
            //ty.volume() >= minVolumeNecessary
                ) {

            if (op.temporal) {
                int sdur = subst.dur;
                if (sdur >= 0) {
                    if (!Compound.matchTemporalDT(this, y, sdur))
                        return false;
                }
            }

            TermContainer xsubs = subterms();
            TermContainer ysubs = y.subterms();


            if (op() == CONJ) { //non-commutive, temporal CONJ
                return TermContainer.unifyConj(xsubs, dt(), ysubs, y.dt(), subst);
            } else if (commutative) {
                return xsubs.unifyCommute(ysubs, subst);
            } else {
                return /*xsubs.equals(ysubs) || */xsubs.unifyLinear(ysubs, subst);
            }

        } else if (y instanceof AliasConcept.AliasAtom) {
            Term abbreviated = ((AliasConcept.AliasAtom) y).target;
            return abbreviated.equals(this) || unify(abbreviated, subst);
        }

        return false;

    }

    abstract protected static class PatternCompoundWithEllipsis extends PatternCompound {

        @NotNull
        final Ellipsis ellipsis;

        PatternCompoundWithEllipsis(@NotNull Op seed, int dt, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(seed, dt, subterms);

            this.ellipsis = ellipsis;

        }

        abstract protected boolean matchEllipsis(@NotNull TermContainer y, @NotNull Unify subst);

        @Override
        public final boolean unify(@NotNull Term y, @NotNull Unify subst) {
            return y.hasAll(structureNecessary) && op() == y.op() && matchEllipsis(y.subterms(), subst);
        }


    }


    public static class PatternCompoundWithEllipsisLinear extends PatternCompoundWithEllipsis {

        public PatternCompoundWithEllipsisLinear(@NotNull Op op, int dt, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(op, dt, ellipsis, subterms);
        }

        @Override
        protected boolean matchEllipsis(@NotNull TermContainer y, @NotNull Unify subst) {
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
        final boolean matchEllipsedLinear(@NotNull TermContainer Y, @NotNull Unify u) {

            int i = 0, j = 0;
            int xsize = sizeCached;
            int ysize = Y.size();

            //TODO check for shim and subtract xsize?

            while (i < xsize) {
                Term x = sub(i++);

                if (x instanceof Ellipsis) {
                    int available = ysize - j;

                    Term eMatched = u.xy(x); //EllipsisMatch, or null
                    if (eMatched == null) {

                        //COLLECT
                        if (i == xsize) {
                            //SUFFIX
                            if (!ellipsis.validSize(available))
                                return false;

                            return ellipsis.unify(EllipsisMatch.match(Y, j, j + available), u);

                        } else {
                            //PREFIX the ellipsis occurred at the start and there are additional terms following it
                            //TODO
                            return false;
                        }
                    } else {
                        //assert(false): "TODO check this case in PatternCompound ellipsis linear";

                        if (eMatched instanceof EllipsisMatch) {
                            EllipsisMatch ex = (EllipsisMatch) eMatched;
                            if (!ex.linearMatch(Y, j, u))
                                return false;
                            j += ex.size();
                        } else {
                            //it is a single ellipsis term to unify against
                            if (!sub(j).unify(eMatched, u))
                                j++;
                        }
                    }
                    //previous match exists, match against what it had
//                        if (i == xsize) {
//                        //SUFFIX - match the remaining terms against what the ellipsis previously collected
//                        //HACK this only works with EllipsisMatch type
//                        Term[] sp = ((EllipsisMatch) eMatched).term;
//                        if (sp.length!=available)
//                            return false; //incorrect size
//
//                        //match every item
//                        for (Term aSp : sp) {
//                            if (!match(aSp, Y.term(j++)))
//                                return false;
//                        }
//                        } else {
//                            //TODO other cases
//                            return false;
//                        }
//                    }
                } else {
                    if (ysize <= j || !x.unify(Y.sub(j++), u))
                        return false;
                }
            }

            return true;
        }


    }


//    /**
//     * requies dt exact match, for example, when matching Images (but not temporal terms)
//     */
//    abstract public static class PatternCompoundWithEllipsisLinearDT extends PatternCompoundWithEllipsisLinear {
//
//        public PatternCompoundWithEllipsisLinearDT(@NotNull Compound seed, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
//            super(seed, ellipsis, subterms);
//        }
//
////        @Override
////        protected boolean canMatch(@NotNull Compound y) {
////            return (dt == y.dt() && super.canMatch(y));
////        }
//
//
//    }

//    public static final class PatternCompoundWithEllipsisLinearImage extends PatternCompoundWithEllipsisLinearDT {
//
//        //private final int ellipseIndex;
//
//        public PatternCompoundWithEllipsisLinearImage(@NotNull Compound seed, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
//            super(seed, ellipsis, subterms);
//            //this.ellipseIndex = indexOf(ellipsis);
//        }
//
////        @Override
////        protected boolean canMatch(@NotNull Compound y) {
////            return super.canMatch(y);
////        }
//
////        @Override
////        protected boolean matchEllipsis(@NotNull Compound y, @NotNull Unify subst) {
////            return matchEllipsisWithImage(y) && super.matchEllipsis(y, subst);
////        }
//
////        public boolean matchEllipsisWithImage(@NotNull Compound y) {
////
////            int xdt = dt();
////
////            //compare relation from beginning as in non-ellipsis case
////            //OR compare relation from end
////            return (ellipseIndex >= xdt) ? (xdt == y.dt()) : ((sizeCached - xdt) == (y.size() - y.dt()));
////
////        }
//
//
//    }

//    /**
//     * does not compare specific image dt
//     */
//    public static final class PatternCompoundWithEllipsisLinearImageTransform extends PatternCompoundWithEllipsisLinear {
//
//        @NotNull
//        private final EllipsisTransform ellipsisTransform;
//
//        public PatternCompoundWithEllipsisLinearImageTransform(@NotNull Compound seed, @NotNull EllipsisTransform ellipsis, @NotNull TermContainer subterms) {
//            super(seed, ellipsis, subterms);
//            this.ellipsisTransform = ellipsis;
//        }
//
//        @Override
//        protected boolean matchEllipsis(@NotNull Compound y, @NotNull Unify subst) {
//
//            //return subst.matchCompoundWithEllipsisTransform(this, (EllipsisTransform) ellipsis, y);
//            //public boolean matchCompoundWithEllipsisTransform(@NotNull Compound X, @NotNull EllipsisTransform et, @NotNull Compound Y) {
//
//            EllipsisTransform et = this.ellipsisTransform;
//            @NotNull Term from = et.from;
//            if (from.equals(Op.Imdex)) {
//                Term n = subst.xy(et.to);
//                if (n != null /*&& !n.equals(y)*/) {
//
//                    //the indicated term should be inserted
//                    //at the index location of the image
//                    //being processed. (this is the opposite
//                    //of the other condition of this if { })
//
//                    return matchEllipsedLinear(y, subst) &&
//                            subst.putXY(et,
//                                    ImageMatch.put(subst.xy(et), n, y));
//
//                }
//            } else {
//                Term n = subst.xy(from);
////                if (n == null) {
////                    //select at random TODO make termutator
////                    int imageIndex = random.nextInt(Y.size());
////                    return (putXY(et.from, Y.term(imageIndex)) && matchEllipsedLinear(X, e, Y)) &&
////                            replaceXY(e, ImageMatch.take(term(e), imageIndex));
////                }
//
//                if (n != null /*&& n.op() != subst.type*/) {
//                    int imageIndex = y.indexOf(n);
//                    if (imageIndex != -1)
//                        return matchEllipsedLinear(y, subst) &&
//                                subst.putXY(et,
//                                        ImageMatch.take(subst.xy(et), imageIndex));
//                }
//            }
//            return false;
//        }
//
//    }

    public static final class PatternCompoundWithEllipsisCommutive extends PatternCompoundWithEllipsis {


//        /** the components of this pattern compound other than the ellipsis "*/
//        final ImmutableSet<Term> fixed;

        public PatternCompoundWithEllipsisCommutive(Op op, int dt, @NotNull Ellipsis ellipsis, @NotNull TermContainer subterms) {
            super(op, dt, ellipsis, subterms);

//            MutableSet<Term> f = new UnifiedSet();
//            subterms.forEach(s -> {
//                if (!s.equals(ellipsis))
//                    f.add(s);
//            });
//            this.fixed = f.toImmutable();

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
        protected boolean matchEllipsis(@NotNull TermContainer y, @NotNull Unify u) {
            //return subst.matchEllipsedCommutative(
            //        this, ellipsis, y
            //);
            //public final boolean matchEllipsedCommutative(@NotNull Compound X, @NotNull Ellipsis Xellipsis, @NotNull Compound Y) {

            SortedSet<Term> xFixed = new TreeSet();//$.newHashSet(0); //Global.newHashSet(0);


            final Ellipsis ellipsis = this.ellipsis;

            SortedSet<Term> yFree = y.toSortedSet();

            int s = size();

            for (int k = 0; k < s; k++) {


                Term x = sub(k);

                if (x.equals(ellipsis)) {
                    Term v = u.xy(x);
                    if (v != null) {
                        if (v instanceof EllipsisMatch) {
                            return ((EllipsisMatch) v).rematch(y, yFree);
                        } else {
                            Term vv = (Term)v; //single-term matched for the ellipsis, so wont be EllipsisMatch instance
                            if (!yFree.remove(vv))
                                return false;
                        }
                    }

                    continue;
                }

                //find (randomly) at least one element of 'y' which unifies with this fixed variable
                //if one matches, remove it from yFree
                //if none match, fail
                //TODO this should be part of the termutator since there could be more than one match
//                int dir = subst.random.nextBoolean() ? +1 : -1;
//                int u = subst.random.nextInt(s);
//                boolean matched = false;
//                for (int w = 0; w < s; w++, u+=dir) {
//                    if (u == -1) u = s - 1;
//                    else if (u == s) u = 0;
//                    //if (!yFree.contains(yu)) continue //?? would this help?
//                    Term yu = y.sub(u);
//                    if (subst.putXY(x, yu)) {
//                        matched = true;
//                        yFree.remove(yu);
//                        break;
//                    }
//                }
//                if (!matched)
//                    return false;

                /*if (v instanceof EllipsisMatch) {

                    //assume it's THE ellipsis here, ie. x == xEllipsis by testing that Y contains all of these
                    if (!((EllipsisMatch) v).addWhileMatching(y, alreadyInY, ellipsis.sizeMin())) {
                        return false;
                    } else {
                        //Xellipsis = null;
                        ellipsisMatched = true;
                        break; //continued below
                    }


                } else */
//                if (v != null) {
//
//                    if (!yFree.remove(v)) {
//                        //required but not actually present in Y
//                        return false;
//                    }
//
//                } else {

                Op xo = x.op();
                boolean xConst = xo!=NEG && !u.matchType(xo); //HACK exception for NEG
                if (!xConst) {
                    //try to resolve an already assigned and thus resolvable to constant
                    @Nullable Term xx = u.xy(x);
                    if (xx!=null) {
                        x = xx;
                        xConst = !u.matchType(xo);
                    }
                }

                if (xConst && !yFree.remove(x))
                    return false; //x requires a constant not even present in y
                if (!xConst) {
                    xFixed.add(x); //x is a variable which must be termuted when everything non-X is assigned
                }

                //         }


            }

            int xs = xFixed.size();
            int ys = yFree.size();
            int numRemainingForEllipsis = ys - xs;

            //if not invalid size there wouldnt be enough remaining matches to satisfy ellipsis cardinality
            boolean vs = ellipsis.validSize(numRemainingForEllipsis);
            if (!vs)
                return false;

            if (xs == 0) {
                return u.putXY(ellipsis, ys > 0 ? EllipsisMatch.match(yFree) : EllipsisMatch.empty);
            } else {
                return matchEllipsisCommutive(u, xFixed, yFree);
            }


        }

        /**
         * toMatch matched into some or all of Y's terms
         */
        boolean matchEllipsisCommutive(@NotNull Unify subst, @NotNull SortedSet<Term> xFixed, @NotNull SortedSet<Term> yFree) {
            int xs = xFixed.size();

            switch (xs) {
                case 0:
                    //match everything
                    return ellipsis.unify(EllipsisMatch.match(yFree), subst);

                case 1:
                    Term x0 = xFixed.first();
                    if (yFree.size() == 1) {
                        return ellipsis.unify(EllipsisMatch.empty, subst) && x0.unify(yFree.first(), subst);
                    } else {
                        return subst.termutes.add(new Choose1(ellipsis, x0, yFree));
                    }

                case 2:
                    return subst.termutes.add(new Choose2(ellipsis, subst, xFixed, yFree));

                default:
                    //3 or more combination
                    throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
            }

        }
    }

    public static final class PatternCompoundSimple extends PatternCompound {

//        private final int subStructureCached;
//        private final boolean commutative;

        public PatternCompoundSimple(Op op, int dt, @NotNull TermContainer subterms) {
            super(op, dt, subterms);
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
