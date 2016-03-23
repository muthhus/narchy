package nars.concept;

import nars.bag.Bag;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;

import java.util.function.Function;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends Function<Term, Concept> {

    Bag<Task> taskbag();
    Bag<Termed> termbag();

}
