package nars.concept;

import nars.NAR;

/**
 * Marker interface indicating the Concept should not be forgettable
 */
public interface PermanentConcept extends Concept {

    @Override
    default void delete(NAR nar) {
        throw new RuntimeException("permanent concept deleted: " + this);
    }

}
