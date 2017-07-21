package nars.index.term;

import nars.Narsese;
import nars.Op;
import nars.derive.PatternCompound;
import nars.derive.match.Ellipsis;
import nars.derive.rule.PremiseRule;
import nars.index.term.map.MapTermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Index which specifically holds the term components of a deriver ruleset.
 */
public class PatternTermIndex extends MapTermIndex {

    public PatternTermIndex() {
        super(new HashMap<>(512));
    }


    @Override
    public Termed get(@NotNull Term x, boolean createIfMissing) {
        if (x instanceof Variable)
            return x;

        Termed y = concepts.get(x);
        if (y == null) {
            concepts.put(x, y = x instanceof Compound ? compute((Compound) x) : x);
        }
        return y;
    }


    @NotNull
    protected Term compute(@NotNull Compound x) {


        TermContainer s = x.subterms();
        int ss = s.size();
        Term[] bb = new Term[ss];
        boolean changed = false;//, temporal = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.sub(i);

            Termed b;
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
            b = get(a, true);
//            }
            if (a != b) {
                changed = true;
            }
            bb[i] = b.term();
        }


        TermContainer v = (changed ? TermVector.the(TermContainer.theTermArray(x.op(), x.dt(), bb)) : s);

        Ellipsis e = Ellipsis.firstEllipsis(v);
        return e != null ?
                makeEllipsis(x, v, e) :
                new PatternCompound.PatternCompoundSimple(x, v);
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
    private static PatternCompound makeEllipsis(@NotNull Compound seed, @NotNull TermContainer v, @NotNull Ellipsis e) {


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

            return new PatternCompound.PatternCompoundWithEllipsisCommutive(seed, e, v);
        } else {
//            if (ellipsisTransform) {
//                if (op != Op.PROD)
//                    throw new RuntimeException("imageTransform ellipsis must be in an Image or Product compound");
//
//                return new PatternCompound.PatternCompoundWithEllipsisLinearImageTransform(
//                        seed, (EllipsisTransform)e, v);
//            } else {
            return new PatternCompound.PatternCompoundWithEllipsisLinear(seed, e, v);
//            }
        }

    }

    /**
     * returns an normalized, optimized pattern term for the given compound
     */
    public @NotNull Compound pattern(@NotNull Compound x) {

        Term y = x.transform(new PremiseRule.PremiseRuleVariableNormalization());

        assert(y!=null);

        return (Compound) get(y, true).term();

    }

    public @NotNull Compound pattern(@NotNull String s) throws Narsese.NarseseException {
        return pattern( (Compound) Narsese.the().term(s, false) );
    }

}
