package nars.concept;

import nars.bag.Bag;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends Function<Term, Concept> {

    @NotNull
    Bag<Task> taskbag();
    @NotNull
    Bag<Termed> termbag();

}
