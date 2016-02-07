package nars.concept;

import nars.NAR;
import nars.bag.BLink;
import nars.task.Task;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Default implementation of a ConceptProcess procedure
 */
public class DefaultConceptProcess extends ConceptProcess {


    /**
     * holds the resulting tasks of one derivation so they can
     * be normalized or some other filter or aggregation
     * applied collectively.
     */
    public final @NotNull Collection<Task> results;

    public DefaultConceptProcess(NAR nar, BLink<? extends Concept> conceptLink,
                                 BLink<? extends Task> taskLink,
                                 BLink<? extends Termed> termLink, @Nullable Task belief, @NotNull Collection<Task> results) {
        super(nar, conceptLink, taskLink, termLink, belief);
        this.results = results;
    }

    @Override
    protected void accept(Task derivation) {
        results.add(derivation);
    }

    @Override
    protected void commit() {


        Collection<Task> buffer = results;

        if (!buffer.isEmpty()) {
//                Task.inputNormalized( buffer,
//                        //p.getMeanPriority()
//                        p.task().pri()
//
//                        //p.getTask().getPriority() * 1f/buffer.size()
//                        //p.getTask().getPriority()/buffer.size()
//                        //p.taskLink.getPriority()
//                        //p.getTaskLink().getPriority()/buffer.size()
//
//                        //p.conceptLink.getPriority()
//                        //UtilityFunctions.or(p.conceptLink.getPriority(), p.taskLink.getPriority())
//
//                ,nar::input);
            nar.input(buffer);
            buffer.clear();
        }

    }
}
