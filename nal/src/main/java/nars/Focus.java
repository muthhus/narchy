package nars;

import jcog.pri.PLink;
import nars.concept.Concept;
import nars.term.Termed;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

/**
 * Created by me on 12/27/16.
 */
public interface Focus extends Iterable<PLink<Concept>> {

    /**
     * if the concept is active, returns the concept link after
     * applying the positive or negative boost factor to its budget
     *
     * otherwise returns null
     */

    @Nullable PLink<Termed> activate(@NotNull Termed term, float priToAdd);


    /**
     * @return current priority of the concept, or NaN if concept isnt active
     */
    float pri(@NotNull Termed concept);

    default float pri(@NotNull Termed concept, float valueIfInactive) {
        float p = pri(concept);
        return (p == p) ? p : valueIfInactive;
    }

    @NotNull Iterable<PLink<Concept>> concepts();

    @NotNull
    @Override
    default Iterator<PLink<Concept>> iterator() {
        return concepts().iterator();
    }



    void sample(int max, @NotNull IntObjectToIntFunction<? super PLink<Concept>> c);

    Focus NULL_FOCUS = new Focus() {

        @Override
        public PLink<Termed> activate(Termed term, float priToAdd) {
            return null;
        }

        @Override
        public void sample(int max, IntObjectToIntFunction<? super PLink<Concept>> c) {

        }

        @Override
        public float pri(@NotNull Termed concept) {
            return 0;
        }

        @Override
        public Iterable<PLink<Concept>> concepts() {
            return Collections.emptyList();
        }
    };


}
