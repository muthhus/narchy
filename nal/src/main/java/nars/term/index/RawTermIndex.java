package nars.term.index;

import nars.concept.ConceptBuilder;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * Term index which stores raw terms (no concepts/conceptualization)
 */
public abstract class RawTermIndex extends MapIndex2 implements Serializable {

    public RawTermIndex(SymbolMap symbolMap, Map<TermContainer, SubtermNode> data, TermBuilder termBuilder, ConceptBuilder conceptBuilder) {
        super(symbolMap, data, termBuilder, conceptBuilder);
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
