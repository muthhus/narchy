package nars.index.term;

import nars.Op;
import nars.concept.util.ConceptBuilder;
import nars.index.term.map.MapTermIndex;
import nars.nal.meta.PatternCompound;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisTransform;
import nars.nal.rule.PremiseRule;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

import static nars.Op.NEG;

/**
 * Index which specifically holds the term components of a deriver ruleset.
 */
public class PatternTermIndex extends MapTermIndex {

    @Deprecated public PatternTermIndex() {
        this(1024);
    }

    public PatternTermIndex(int capacity) {
        super(ConceptBuilder.Null,
            new ConcurrentHashMap<>(capacity),
            new ConcurrentHashMap<>(capacity));

    }

    @Override
    public Termed get(Term x, boolean createIfMissing) {
        if (x instanceof Compound) {
            return compute((Compound)x);
        }
        return super.get(x, createIfMissing);
    }


    @NotNull
    protected Termed compute(@NotNull Compound x) {

        //dont store the actual rules, they are guaranteed unique by other means
        if (x instanceof PremiseRule) {
            return x;
        }

        TermContainer s = x.subterms();
        int ss = s.size();
        Term[] bb = new Term[ss];
        boolean changed = false;//, temporal = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.term(i);

            Term b;
            if (a instanceof Compound) {

                if (!canBuildConcept(a) || a.hasTemporal()) {
                    //temporal = true;//dont store subterm arrays containing temporal compounds
                    b = a;
                } else {
                    /*if (b != a && a.isNormalized())
                        ((GenericCompound) b).setNormalized();*/
                    b = get(a, true).term();
                }
            } else {
                b = get(a, true).term();
            }
            if (a != b) {
                changed = true;
            }
            bb[i] = b;
        }

        TermContainer v = (changed ? TermContainer.the(x.op(), bb) : s);


        Ellipsis e = Ellipsis.firstEllipsis(v);
        return e != null ?
                makeEllipsis(x, v, e) :
                new PatternCompound.PatternCompoundSimple(x, v);
    }

    static protected boolean canBuildConcept(@NotNull Term y) {
        if (y instanceof Compound) {
            return y.op() != NEG;
        } else {
            return !(y instanceof Variable);
        }

    }


    @NotNull
    private static PatternCompound makeEllipsis(@NotNull Compound seed, @NotNull TermContainer v, @NotNull Ellipsis e) {


        //this.ellipsisTransform = hasEllipsisTransform(this);
        boolean hasEllipsisTransform = false;
        int xs = seed.size();
        for (int i = 0; i < xs; i++) {
            if (seed.term(i) instanceof EllipsisTransform) {
                hasEllipsisTransform = true;
                break;
            }
        }

        Op op = seed.op();

        boolean ellipsisTransform = hasEllipsisTransform;
        boolean commutative = (!ellipsisTransform && op.commutative);

        if (commutative) {
            if (ellipsisTransform)
                throw new RuntimeException("commutative is mutually exclusive with ellipsisTransform");

            return new PatternCompound.PatternCompoundWithEllipsisCommutive(seed, e, v);
        } else {
            if (ellipsisTransform) {
                if (!op.image && op != Op.PROD)
                    throw new RuntimeException("imageTransform ellipsis must be in an Image or Product compound");

                return new PatternCompound.PatternCompoundWithEllipsisLinearImageTransform(
                        seed, (EllipsisTransform)e, v);
            } else if (op.image) {
                return new PatternCompound.PatternCompoundWithEllipsisLinearImage(seed, e, v);
            } else {
                return new PatternCompound.PatternCompoundWithEllipsisLinear(seed, e, v);
            }
        }

    }


}
