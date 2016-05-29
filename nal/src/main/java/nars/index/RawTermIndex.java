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
                new HashMap<>(capacity),
                new HashMap<>(capacity) );
    }


    /**
     * doesnt conceptualize, pass-through raw term
     */
    @NotNull @Override protected Termed build(@NotNull Termed interned) {
        return interned;
    }

}
