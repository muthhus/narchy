package nars.concept;

import nars.bag.Bag;
import nars.task.Task;
import nars.term.Termed;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class VariableConcept extends AtomConcept<Variable> implements Variable {

    public VariableConcept(@NotNull Variable atom, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        super(atom, termLinks, taskLinks);
    }

    @Override
    public final int id() {
        return term.id();
    }
}
