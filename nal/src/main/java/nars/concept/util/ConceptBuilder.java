package nars.concept.util;

import nars.NAR;
import nars.Task;
import nars.budget.policy.ConceptPolicy;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends Function<Term, Termed> {




    @NotNull ConceptPolicy init();
    @NotNull ConceptPolicy awake();
    @NotNull ConceptPolicy sleep();

    void start(NAR nar);


    ConceptBuilder Null = new NullConceptBuilder();


    /** passes through terms without creating any concept anything */
    final class NullConceptBuilder implements ConceptBuilder {

        @Override
        public Termed apply(Term term) {
            return term;
        }

        @Override
        public @NotNull ConceptPolicy init() {
            return null;
        }

        @Override
        public @NotNull ConceptPolicy awake() {
            return null;
        }

        @Override
        public @NotNull ConceptPolicy sleep() {
            return null;
        }

        @Override
        public void start(NAR nar) {

        }

    }
}
