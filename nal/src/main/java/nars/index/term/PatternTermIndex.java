package nars.index.term;

import nars.Narsese;
import nars.Op;
import nars.derive.PatternCompound;
import nars.derive.match.Ellipsis;
import nars.index.term.map.MapTermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.container.TermContainer;
import nars.term.transform.VariableNormalization;
import nars.term.var.AbstractVariable;
import nars.term.var.Variable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

import static nars.$.v;
import static nars.Op.VAR_PATTERN;
import static nars.Op.concurrent;

/**
 * Index which specifically holds the term components of a deriver ruleset.
 */
public class PatternTermIndex extends MapTermIndex {

    public PatternTermIndex() {
        super(new ConcurrentHashMap<>(512));
    }

    @SuppressWarnings("Java8MapApi")
    @Override
    public Termed get(@NotNull Term x, boolean createIfMissing) {
        if (x instanceof Variable)
            return x;

        Termed y = concepts.get(x);
        if (y == null) {
            concepts.put(x, y = x instanceof Compound ? patternify((Compound) x) : x);
        }
        return y;
    }


    @NotNull
    protected Term patternify(@NotNull Compound x) {


        TermContainer s = x.subterms();
        int ss = s.size();
        Term[] bb = new Term[ss];
        boolean changed = false;//, temporal = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.sub(i);

            //            Termed b;
//            if (a instanceof Compound) {
//
//                if (!canBuildConcept(a) || a.isTemporal()) {
//                    //temporal = true;//dont store subterm arrays containing temporal compounds
//                    b = a;
//                } else {
//                    /*if (b != a && a.isNormalized())
//                        ((GenericCompound) b).setNormalized();*/
//                    b = get(a, true);
//                }
//            } else {
            Termed b = get(a, true);
//            }
            if (a != b) {
                changed = true;
            }
            bb[i] = b.term();
        }


        TermContainer v = (changed ? Op.subterms(bb.length > 1 && x.op().commutative && (concurrent(x.dt())) ?
                Terms.sorted(bb) :
                bb) : s);

        Ellipsis e = Ellipsis.firstEllipsis(v);
        return e != null ?
                ellipsis(x, v, e) :
                new PatternCompound.PatternCompoundSimple(x.op(), x.dt(), v);
    }

//    static boolean canBuildConcept(@NotNull Term y) {
//        if (y instanceof Compound) {
//            return y.op() != NEG;
//        } else {
//            return !(y instanceof Variable);
//        }
//
//    }


    @NotNull
    private static PatternCompound ellipsis(@NotNull Compound seed, @NotNull TermContainer v, @NotNull Ellipsis e) {


        //this.ellipsisTransform = hasEllipsisTransform(this);
        //boolean hasEllipsisTransform = false;
        //int xs = seed.size();
//        for (int i = 0; i < xs; i++) {
//            if (seed.sub(i) instanceof EllipsisTransform) {
//                hasEllipsisTransform = true;
//                break;
//            }
//        }

        Op op = seed.op();

        //boolean ellipsisTransform = hasEllipsisTransform;
        boolean commutative = (/*!ellipsisTransform && */op.commutative);

        if (commutative) {
//            if (ellipsisTransform)
//                throw new RuntimeException("commutative is mutually exclusive with ellipsisTransform");

            return new PatternCompound.PatternCompoundWithEllipsisCommutive(seed.op(), seed.dt(), e, v);
        } else {
//            if (ellipsisTransform) {
//                if (op != Op.PROD)
//                    throw new RuntimeException("imageTransform ellipsis must be in an Image or Product compound");
//
//                return new PatternCompound.PatternCompoundWithEllipsisLinearImageTransform(
//                        seed, (EllipsisTransform)e, v);
//            } else {
            return new PatternCompound.PatternCompoundWithEllipsisLinear(seed.op(), seed.dt(), e, v);
//            }
        }

    }

    /**
     * returns an normalized, optimized pattern term for the given compound
     */
    public @NotNull Compound pattern(@NotNull Compound x) {

        Term y = x.transform(new PremiseRuleVariableNormalization());

        assert(y!=null);

        return (Compound) get(y, true).term();

    }

    public @NotNull Compound pattern(@NotNull String s) throws Narsese.NarseseException {
        return pattern( (Compound) Narsese.parse().term(s, false) );
    }

    public static final class PremiseRuleVariableNormalization extends VariableNormalization {


        public static final int ELLIPSIS_ZERO_OR_MORE_ID_OFFSET = 1 * 256;
        public static final int ELLIPSIS_ONE_OR_MORE_ID_OFFSET = 2 * 256;
        public static final int ELLIPSIS_TRANSFORM_ID_OFFSET = 3 * 256;

        public PremiseRuleVariableNormalization() {
            super(new UnifiedMap<>());
        }

        public static AbstractVariable varPattern(int i) {
            return v(VAR_PATTERN, i);
        }

        @NotNull
        @Override
        protected Variable newVariable(@NotNull Variable x) {


            int actualSerial = count;

//            if (x instanceof Ellipsis.EllipsisTransformPrototype) {
//                //special
//
//                Ellipsis.EllipsisTransformPrototype ep = (Ellipsis.EllipsisTransformPrototype) x;
//
////                Term from = ep.from;
////                if (from != Op.Imdex) from = applyAfter((GenericVariable)from);
////                Term to = ep.to;
////                if (to != Op.Imdex) to = applyAfter((GenericVariable)to);
////
//                return EllipsisTransform.make(varPattern(actualSerial + ELLIPSIS_TRANSFORM_ID_OFFSET), ep.from, ep.to, this);
//
//            } else
            if (x instanceof Ellipsis.EllipsisPrototype) {
                Ellipsis.EllipsisPrototype ep = (Ellipsis.EllipsisPrototype) x;
                return Ellipsis.EllipsisPrototype.make(actualSerial +
                                (ep.minArity == 0 ? ELLIPSIS_ZERO_OR_MORE_ID_OFFSET : ELLIPSIS_ONE_OR_MORE_ID_OFFSET) //these need to be distinct
                        , ep.minArity);
            } else if (x instanceof Ellipsis) {

                return x;

                //throw new UnsupportedOperationException("?");
//                int idOffset;
//                if (v instanceof EllipsisTransform) {
//                    idOffset = ELLIPSIS_TRANSFORM_ID_OFFSET;
//                } else if (v instanceof EllipsisZeroOrMore) {
//                    idOffset = ELLIPSIS_ZERO_OR_MORE_ID_OFFSET;
//                } else if (v instanceof EllipsisOneOrMore) {
//                    idOffset = ELLIPSIS_ONE_OR_MORE_ID_OFFSET;
//                } else {
//                    throw new RuntimeException("N/A");
//                }
//
//                Variable r = ((Ellipsis) v).clone(varPattern(actualSerial + idOffset), this);
//                offset = 0; //return to zero
//                return r;
            } /*else if (v instanceof GenericVariable) {
                return ((GenericVariable) v).normalize(actualSerial); //HACK
            } else {
                return v(v.op(), actualSerial);
            }*/
            return super.newVariable(x);
        }

//        @Override
//        public final boolean testSuperTerm(@NotNull Compound t) {
//            //descend all, because VAR_PATTERN is not yet always considered a variable
//            return true;
//        }

        @NotNull
        public Term applyAfter(@NotNull Variable secondary) {
            if (secondary.equals(Op.Imdex))
                return secondary; //dont try to normalize any imdex
            else
                return apply(null, secondary);
        }
    }
}
