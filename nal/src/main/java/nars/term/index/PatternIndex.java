package nars.term.index;

import nars.nal.meta.PatternCompound;
import nars.nal.meta.PremiseRule;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.util.data.map.UnifriedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.$.$;

/**
 * Index which specifically holds the term components of a deriver ruleset.
 */
public class PatternIndex extends MapIndex2 {

    public PatternIndex() {
        super(new UnifriedMap(512), null);
    }

    @Override Termed theAtom(@NotNull Atomic t, boolean createIfMissing) {
        SymbolMap a = this.atoms;
        return (createIfMissing ? a.resolveOrAdd(t, u -> u /* pass through */) : a.resolve(t)) ;
    }

    @NotNull
    @Override
    protected Termed internCompound(Termed interned) {
        return interned; //dont conceptualize, pass-through raw term
    }

    //public transient int internings = 0; //temporary counter for statistics

    @Override
    protected @Nullable Termed theCompound(@NotNull Compound t, boolean create) {

        //dont store the actual rules, they are guaranteed unique by other means
        if (t instanceof PremiseRule) {
            return t;
        }

        //internings++;
        return super.theCompound(PatternCompound.make(t,
            theSubterms(t.subterms())
        ), true);

    }


}
