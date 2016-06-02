package nars.index;

import nars.concept.Concept;
import nars.term.TermBuilder;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Term index which stores raw terms (no concepts/conceptualization)
 */
public abstract class RawTermIndex extends MapIndex implements Serializable {

    public RawTermIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder, int capacity) {
        super(termBuilder, conceptBuilder,
                new ConcurrentHashMap<>(capacity),
                new ConcurrentHashMap<>(capacity)
//                new HashMap<>(capacity),
//                new HashMap<>(capacity)
        );
    }


    /**
     * doesnt conceptualize, pass-through raw term
     */
    @NotNull @Override protected Termed build(@NotNull Termed interned) {
        return interned;
    }

}
