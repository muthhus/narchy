/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nal;

import nars.NAR;
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
 *
 *
 */
public final class Premise implements Tasked {


    @NotNull public final Task taskLink;


    @NotNull public final Term termLink;

    @Nullable public final Task belief;

    /** not used in creating a Premise key, because the same premise components may be generated from different originating concepts or even other methods of forming them*/
    @NotNull transient private final Term conceptLink;

    public Premise(@NotNull Term conceptLink,
                   @NotNull Task taskLink,
                   @NotNull Term termLink,
                   @Nullable Task belief) {

        this.taskLink = taskLink;

        this.conceptLink = conceptLink;

        this.termLink = termLink;

        this.belief = belief;
    }


    @NotNull
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

    @Nullable public final Concept concept(NAR n) {
        return n.concept(conceptLink);
    }

    @Nullable public final BLink<? extends Termed> termlink(@NotNull Concept c) {
        return c.termlinks().get(termLink);
    }

    @Nullable public final BLink<? extends Task> tasklink(@NotNull Concept c) {
        return c.tasklinks().get(taskLink);
    }
}
