package nars.index;

import nars.concept.Concept;
import nars.nar.util.DefaultConceptBuilder;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Term index which stores raw terms (no concepts/conceptualization)
 */
public abstract class RawTermIndex extends SimpleMapIndex2 implements Serializable {

    public RawTermIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder, int capacity) {
        super(termBuilder, conceptBuilder,
                new HashMap(capacity),
                new HashMap(capacity) );
    }

    @Override
    Termed theAtom(@NotNull Atomic t, boolean createIfMissing) {
        SymbolMap a = this.atoms;
        return (createIfMissing ? a.resolveOrAdd(t, u -> u /* pass through */) : a.resolve(t)) ;
    }

    @NotNull
    @Override
    protected Termed internCompound(@NotNull Termed interned) {
        return interned; //dont conceptualize, pass-through raw term
    }



}
