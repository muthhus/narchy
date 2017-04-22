package nars;

import jcog.bag.Bag;
import jcog.pri.PLink;
import nars.concept.Concept;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

/**
 * Created by me on 12/27/16.
 */
public interface Focus extends Iterable<PLink<Concept>> {

    @NotNull PLink<Concept> activate(@NotNull Concept term, float priToAdd);

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

    void sample(@NotNull Bag.BagCursor<? super PLink<Concept>> c);

    Focus NULL_FOCUS = new Focus() {

        @Override
        public PLink<Concept> activate(Concept term, float priToAdd) {
            return null;
        }


        @Override
        public float pri(@NotNull Termed concept) {
            return 0;
        }

        @Override
        public Iterable<PLink<Concept>> concepts() {
            return Collections.emptyList();
        }

        @Override
        public void sample(@NotNull Bag.@NotNull BagCursor<? super PLink<Concept>> c) {

        }
    };


}
