/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nal;

import nars.Premise;
import nars.concept.Concept;
import nars.link.BLink;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Firing a concept (reasoning event).  Derives new Tasks via reasoning rules.
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops
 */
public class ConceptProcess implements Premise {

    @NotNull public final BLink<? extends Task> taskLink;

    @NotNull public final BLink<? extends Term> termLink;

    @Nullable public final Task belief;

    @NotNull public final BLink<? extends Concept> conceptLink;


    public ConceptProcess(BLink<? extends Concept> conceptLink,
                          BLink<? extends Task> taskLink,
                          BLink<? extends Term> termLink, @Nullable Task belief) {

        this.taskLink = taskLink;

        this.conceptLink = conceptLink;

        this.termLink = termLink;

        this.belief = belief;
    }


    @Nullable
    @Override
    public final Task task() {
        return taskLink.get();
    }


    @NotNull
    @Override
    public final Termed beliefTerm() {
        Task x = belief();
        return x == null ? termLink.get() : x;
    }

    @Nullable
    @Override
    public final Task belief() {
        return belief;
    }


    @NotNull
    @Override
    public String toString() {
        return new StringBuilder().append(
                getClass().getSimpleName())
                .append('[')
                //.append(conceptLink).append(',')
                .append(taskLink).append(',')
                .append(termLink).append(',')
                .append(belief())
                .append(']')
                .toString();
    }


}
