package nars.index.term;

import nars.concept.builder.ConceptBuilder;
import nars.concept.builder.DefaultConceptBuilder;
import nars.index.term.map.MapTermIndex;

import java.util.HashMap;

/**
 * suitable for single-thread, testing use only. provides no limitations on size so it will grow unbounded. use with caution
 */
public class BasicTermIndex extends MapTermIndex {

    public BasicTermIndex(int capacity) {
        this(capacity, new DefaultConceptBuilder());
    }

    public BasicTermIndex(int capacity, ConceptBuilder cb) {
        super(
                new HashMap<>(capacity/*, 0.9f*/)
                //new UnifiedMap(capacity, 0.9f),
                //new UnifiedMap(capacity, 0.9f)
                //new ConcurrentHashMap<>(capacity),
                //new ConcurrentHashMap<>(capacity)
                //new ConcurrentHashMapUnsafe(capacity)
        );
    }
}
