package nars.nal.meta;

import nars.Op;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisOneOrMore;
import nars.nal.meta.match.EllipsisTransform;
import nars.term.Compound;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

abstract public class PatternCompound extends GenericCompound {

    public final int sizeCached;
    public final int volCached;
    public final int structureCached;
    public final Term[] termsCached;


    protected static final class PatternCompoundContainingEllipsis extends PatternCompound {

        protected final Ellipsis ellipsis;
        private final boolean ellipsisTransform;

        PatternCompoundContainingEllipsis(@NotNull Compound seed, @NotNull TermVector subterms) {
            super(seed, subterms);

            this.ellipsis = seed.firstEllipsis();
            this.ellipsisTransform = hasEllipsisTransform(this);
        }

        @Override
        public Ellipsis firstEllipsis() {
            return ellipsis;
        }

        @Override
        public boolean match(@NotNull Compound y, @NotNull FindSubst subst) {
            return canMatch(y) && ((ellipsis == null) ?
                    ((y.isCommutative()) ?
                            subst.matchPermute(this, y) :
                            subst.matchLinear(this, y)) :
                    subst.matchCompoundWithEllipsis(this, y));
        }

        @Override protected final boolean canMatch(@NotNull Compound y) {

            int yStructure = y.structure();
            if ((yStructure | structureCached) != yStructure)
                return false;

            Ellipsis e = this.ellipsis;

            boolean eNull = (e == null);
            //in ellipsis zero or more, the size and volume may be less if zero are matched
            if (eNull && sizeCached != y.size())
                return false;

            if (eNull || (e instanceof EllipsisOneOrMore)) {

                //since ellipsisTransform instanceof EllipsisOneOrMore
                if ((volCached > y.volume()) ||
                        (!ellipsisTransform && (relation != y.relation())))
                    return false;
            }

            return true;
        }

    }

    protected static final class PatternCompoundSimple extends PatternCompound {

        PatternCompoundSimple(@NotNull Compound seed, @NotNull TermVector subterms) {
            super(seed, subterms);
        }

        @Override
        public Ellipsis firstEllipsis() {
            return null;
        }

        @Override
        public boolean match(@NotNull Compound y, @NotNull FindSubst subst) {
            return canMatch(y) &&
                    ((y.isCommutative()) ?
                            subst.matchPermute(this, y) :
                            subst.matchLinear(this, y));
        }

        @Override protected final boolean canMatch(@NotNull Compound y) {

            int yStructure = y.structure();

            return  ((yStructure | structureCached) == yStructure) &&
                    (sizeCached == y.size()) &&
                    (volCached <= y.volume()) &&
                    (relation == y.relation());
        }


    }

    public static PatternCompound make(Compound seed) {
        return make(seed, (TermVector)seed.subterms());
    }

    public static PatternCompound make(Compound seed, TermVector v) {
        if (seed.firstEllipsis()!=null) {
            return new PatternCompoundContainingEllipsis(seed, v);
        } else {
            return new PatternCompoundSimple(seed, v);
        }
    }


    PatternCompound(@NotNull Compound seed) {
        this(seed, (TermVector) seed.subterms());
    }

    PatternCompound(@NotNull Compound seed, @NotNull TermVector subterms) {
        super(seed.op(), seed.relation(), subterms);

        sizeCached = seed.size();
        structureCached =
                //seed.structure() & ~(Op.VariableBits);
                seed.structure() & ~(Op.VAR_PATTERN.bit());
        this.volCached = seed.volume();
        this.termsCached = subterms.terms();
    }

    public static boolean hasEllipsisTransform(@NotNull TermContainer x) {
        int xs = x.size();
        for (int i = 0; i < xs; i++)
            if (x.term(i) instanceof EllipsisTransform) return true;
        return false;
    }



    @Override
    public Term[] terms() {
        return termsCached;
    }

    @Override
    public final int structure() {
        return structureCached;
    }

    abstract public boolean match(@NotNull Compound y, @NotNull FindSubst subst);
    abstract protected boolean canMatch(@NotNull Compound y);


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
