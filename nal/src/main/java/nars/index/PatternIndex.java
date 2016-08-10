package nars.index;

import nars.Op;
import nars.nal.meta.PatternCompound;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisTransform;
import nars.nal.rule.PremiseRule;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Index which specifically holds the term components of a deriver ruleset.
 */
public class PatternIndex extends RawTermIndex {

//    public PatternIndex() {
//        super(new HashSymbolMap(
//              new ConcurrentHashMapUnsafe(512)),
//              new ConcurrentHashMapUnsafe(2048), Terms.terms, null);
//    }
    public PatternIndex() {
        super(null, 1024);

        loadBuiltins();
    }

    @Override
    protected @Nullable
    Termed theCompound(@NotNull Compound t, boolean createIfMissing) {

        //dont store the actual rules, they are guaranteed unique by other means
        if (t instanceof PremiseRule) {
            return t;
        }

        //process Patterns
        return make(t);

    }

    @NotNull
    private PatternCompound make(@NotNull Compound seed) {

        TermContainer v = theSubterms(seed.subterms());
        //TermContainer vv = v; //TermVector.the(v);

        Ellipsis e = Ellipsis.firstEllipsis(v);
        return e != null ?
                makeEllipsis(seed, v, e) :
                new PatternCompound.PatternCompoundSimple(seed, v);
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
                if (!op.isImage() && op != Op.PROD)
                    throw new RuntimeException("imageTransform ellipsis must be in an Image or Product compound");

                return new PatternCompound.PatternCompoundWithEllipsisLinearImageTransform(
                        seed, (EllipsisTransform)e, v);
            } else if (op.isImage()) {
                return new PatternCompound.PatternCompoundWithEllipsisLinearImage(seed, e, v);
            } else {
                return new PatternCompound.PatternCompoundWithEllipsisLinear(seed, e, v);
            }
        }

    }



    @Override
    protected final boolean transformImmediates() {
        return false;
    }

}
