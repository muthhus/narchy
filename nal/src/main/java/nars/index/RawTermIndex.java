package nars.index;

import nars.concept.util.ConceptBuilder;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Term index which stores raw terms (no concepts/conceptualization)
 */
public abstract class RawTermIndex extends MapIndex implements Serializable {

    RawTermIndex(int capacity) {
        super(ConceptBuilder.Null,
                new ConcurrentHashMap<>(capacity),
                new ConcurrentHashMap<>(capacity)
//                new HashMap<>(capacity),
//                new HashMap<>(capacity)
        );
    }


}
