package nars;

import nars.concept.Concept;
import nars.link.BLink;
import nars.term.Termed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 12/27/16.
 */
public interface Control {

    /**
     * if the concept is active, returns the Concept while applying the boost factor to its budget
     */

    void activate(Termed term, float priToAdd);


    /**
     * @return current priority of the concept, or NaN if concept isnt active
     */
    float pri(@NotNull Termed concept);

    default float pri(@NotNull Termed concept, float valueIfInactive) {
        float p = pri(concept);
        return (p == p) ? p : valueIfInactive;
    }

    Iterable<? extends BLink<Concept>> conceptsActive();

}
