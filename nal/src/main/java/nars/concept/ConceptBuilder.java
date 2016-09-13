package nars.concept;

import nars.NAR;
import nars.Task;
import nars.budget.policy.ConceptPolicy;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends Function<Term, Termed> {


    @NotNull ConceptPolicy init();
    @NotNull ConceptPolicy awake();
    @NotNull ConceptPolicy sleep();

    void start(NAR nar);

    default void init(Concept c) {
        c.policy(init(), ETERNAL, Task.EmptyTaskList);
    }
}
