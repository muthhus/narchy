/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nal;

import nars.Task;
import nars.budget.RawBudget;
import nars.concept.Concept;
import nars.link.BLink;
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
public final class Premise extends RawBudget implements Tasked {


    //@NotNull private final Term concept;

    @NotNull public final Task task;

    @NotNull public final Term term;

    @Nullable public final Task belief;



    ///** not used in creating a Premise key, because the same premise components may be generated from different originating concepts or even other methods of forming them*/
    //@NotNull transient private final Term conceptLink;


    public Premise(@NotNull Term concept, @NotNull Task taskLink,
                   @NotNull Term termLink,
                   @Nullable Task belief) {

        //this.concept = concept;

        this.task = taskLink;

        //use the belief's term and not the termlink because it is more specific if:
        // a) it contains temporal information that can be used in temporalization
        // b) a variable in the termlink was matched
        this.term = belief!=null ? belief.term() : termLink;

        this.belief = belief;

        //this.conceptLink = conceptLink;

    }



    @NotNull
    @Override
    public final Task task() {
        return task;
    }


    @NotNull
    public final Termed<?> beliefTerm() {
        Task x = belief();
        return x == null ? term : x;
    }

    @Nullable
    public final Task belief() {
        return belief;
    }


    @NotNull
    @Override
    public String toString() {
        return task + " | " + term + " | " + (belief!=null ?  belief : "");
    }


//    /** true if both task and (non-null) belief are temporal events */
//    public final boolean isEvent() {
//        /* TODO This part is used commonly, extract into its own precondition */
//        Task b = belief();
//        return (b!=null) && (!task().isEternal()) && (!b.isEternal());
//    }

//    @Nullable public final Concept concept(NAR n) {
//        return n.concept(conceptLink);
//    }

//    @Nullable public final BLink<? extends Termed> termlink(NAR nar) {
//        return termlink(nar.concept(concept));
//    }

    @Nullable public final BLink<? extends Termed> termlink(Concept c) {
        return c.termlinks().get(term);
    }

//    @Nullable public final BLink<? extends Task> tasklink(NAR nar) {
//        return tasklink(nar.concept(concept));
//    }

    @Nullable public final BLink<? extends Task> tasklink(Concept c) {
        return c.tasklinks().get(task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Premise)) return false;

        Premise premise = (Premise) o;

        if (!task.equals(premise.task)) return false;
        if (!term.equals(premise.term)) return false;
        return belief != null ? belief.equals(premise.belief) : premise.belief == null;

    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + task.hashCode();
        result = 31 * result + term.hashCode();
        result = 31 * result + (belief != null ? belief.hashCode() : 0);
        return result;
    }
}
