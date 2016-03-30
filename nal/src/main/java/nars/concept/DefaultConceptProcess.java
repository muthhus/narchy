package nars.concept;

import nars.NAR;
import nars.bag.BLink;
import nars.task.Task;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Default implementation of a ConceptProcess procedure
 */
public class DefaultConceptProcess extends ConceptProcess {

    public final @NotNull Consumer<Task> results;

    public DefaultConceptProcess(NAR nar, BLink<? extends Concept> conceptLink,
                                 BLink<? extends Task> taskLink,
                                 BLink<? extends Termed> termLink, @Nullable Task belief, @NotNull Consumer<Task> results) {
        super(nar, conceptLink, taskLink, termLink, belief);

        this.results = results;
    }

    @Override
    protected void accept(Task derivation) {
        results.accept(derivation);
    }

}
