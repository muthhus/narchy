/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nal;

import nars.concept.Concept;
import nars.link.BLink;
import nars.task.Task;
import nars.task.Tasked;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 *
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public final class Premise implements Tasked {

    //@NotNull public final BLink<? extends Task> taskLink;
    @NotNull public final Task taskLink;

    //@NotNull public final BLink<? extends Term> termLink;
    @NotNull public final Term termLink;

    @Nullable public final Task belief;

    //@NotNull public final BLink<? extends Concept> conceptLink;
    @NotNull public final Concept conceptLink;

    public Premise(@NotNull Concept conceptLink,
                   @NotNull Task taskLink,
                   @NotNull Term termLink,
                   @Nullable Task belief) {

        this.taskLink = taskLink;

        this.conceptLink = conceptLink;

        this.termLink = termLink;

        this.belief = belief;
    }


    @Nullable
    @Override
    public final Task task() {
        return taskLink;
    }


    @NotNull
    public final Termed beliefTerm() {
        Task x = belief();
        return x == null ? termLink : x;
    }

    @Nullable
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


    /** true if both task and (non-null) belief are temporal events */
    public final boolean isEvent() {
        /* TODO This part is used commonly, extract into its own precondition */
        Task b = belief();
        return (b!=null) && (!task().isEternal()) && (!b.isEternal());
    }

    @Nullable public BLink<? extends Termed> termlink() {
        return conceptLink.termlinks().get(termLink);
    }

    public BLink<? extends Task> tasklink() {
        return conceptLink.tasklinks().get(taskLink);
    }
}
