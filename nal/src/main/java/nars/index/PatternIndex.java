package nars.index;

import nars.$;
import nars.nal.meta.PatternCompound;
import nars.nal.meta.PremiseRule;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisTransform;
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
        super($.terms, null, 1024);
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
    public PatternCompound make(@NotNull Compound seed) {

        TermContainer v = theSubterms(seed.subterms());
        Ellipsis e = Ellipsis.firstEllipsis(v);
        return e != null ?
                makeEllipsis(seed, v, e) :
                new PatternCompound.PatternCompoundSimple(seed, v);
    }

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
        boolean ellipsisTransform = hasEllipsisTransform;
        boolean commutative = (!ellipsisTransform && seed.op().commutative);

        if (commutative) {
            if (ellipsisTransform)
                throw new RuntimeException("commutative is mutually exclusive with ellipsisTransform");

            return new PatternCompound.PatternCompoundWithEllipsisCommutive(seed, e, v);
        } else {
            if (!seed.op().isImage() || ellipsisTransform) {
                return new PatternCompound.PatternCompoundWithEllipsisLinear(seed, e, v);
            } else {
                return new PatternCompound.PatternCompoundWithEllipsisLinearDT(seed, e, v);
            }
        }

    }

}
