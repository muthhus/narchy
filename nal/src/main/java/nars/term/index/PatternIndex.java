package nars.term.index;

import com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import nars.concept.ConceptBuilder;
import nars.nal.meta.PatternCompound;
import nars.nal.meta.PremiseRule;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.util.data.map.UnifriedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static nars.$.$;

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
        super(new HashSymbolMap(
                new HashMap(512)),
                new HashMap(2048), Terms.terms, null);
    }

    @Override
    protected @Nullable
    Termed theCompound(@NotNull Compound t, boolean create) {

        //dont store the actual rules, they are guaranteed unique by other means
        if (t instanceof PremiseRule) {
            return t;
        }

        //process Patterns
        return super.theCompound(
            PatternCompound.make(t,
                theSubterms(t.subterms())
        ), true);

    }

}
