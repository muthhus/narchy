package nars;

import jcog.bag.PLink;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Created by me on 12/27/16.
 */
public interface Control {

    /**
     * if the concept is active, returns the Concept while applying the boost factor to its budget
     */

    void activate(Concept term, float priToAdd);


    /**
     * @return current priority of the concept, or NaN if concept isnt active
     */
    float pri(@NotNull Termed concept);

    default float pri(@NotNull Termed concept, float valueIfInactive) {
        float p = pri(concept);
        return (p == p) ? p : valueIfInactive;
    }

    Iterable<PLink<Concept>> conceptsActive();



    Control NullControl = new Control() {
        @Override
        public void activate(Concept term, float priToAdd) {

        }

        @Override
        public float pri(@NotNull Termed concept) {
            return 0;
        }

        @Override
        public Iterable<PLink<Concept>> conceptsActive() {
            return Collections.emptyList();
        }
    };


}
