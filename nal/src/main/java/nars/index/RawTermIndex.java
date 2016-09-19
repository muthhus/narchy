package nars.index;

import nars.concept.util.ConceptBuilder;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Term index which stores raw terms (no concepts/conceptualization)
 */
public abstract class RawTermIndex extends MapIndex implements Serializable {

    RawTermIndex(ConceptBuilder conceptBuilder, int capacity) {
        super(conceptBuilder,
                new ConcurrentHashMap<>(capacity),
                new ConcurrentHashMap<>(capacity)
//                new HashMap<>(capacity),
//                new HashMap<>(capacity)
        );
    }


    /**
     * doesnt conceptualize, pass-through raw term
     */
    @NotNull @Override protected Termed buildConcept(@NotNull Termed interned) {
        return interned;
    }

}
