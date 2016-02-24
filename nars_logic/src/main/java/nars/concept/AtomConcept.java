package nars.concept;

import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.concept.util.BeliefTable;
import nars.concept.util.TaskTable;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by me on 9/2/15.
 */
public class AtomConcept extends AbstractConcept  {

    protected final Bag<Task> taskLinks;
    protected final Bag<Termed> termLinks;


//    /** creates with no termlink and tasklink ability */
//    public AtomConcept(Term atom, Budget budget) {
//        this(atom, budget, new NullBag(), new NullBag());
//    }

    public AtomConcept(Term atom, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        super(atom);
        this.termLinks = termLinks;
        this.taskLinks = taskLinks;
    }



    /**
     * Task links for indirect processing
     */
    @Override
    public final Bag<Task> tasklinks() {
        return taskLinks;
    }

    /**
     * Term links between the term and its components and compounds; beliefs
     */
    @Override
    public final Bag<Termed> termlinks() {
        return termLinks;
    }


    @Nullable
    @Override
    public BeliefTable beliefs() {
        return BeliefTable.EMPTY;
    }

    @Nullable
    @Override
    public BeliefTable goals() {
        return BeliefTable.EMPTY;
    }

    @Nullable
    @Override
    public TaskTable questions() {
        return BeliefTable.EMPTY;
    }

    @Nullable
    @Override
    public TaskTable quests() {
        return BeliefTable.EMPTY;
    }

    static final String shouldntProcess = "should not have attempted to process task here";

    @Nullable
    @Override
    public Task processBelief(Task task, NAR nar) {
        throw new RuntimeException(shouldntProcess);
    }
    @Nullable
    @Override
    public Task processGoal(Task task, NAR nar) {
        throw new RuntimeException(shouldntProcess);
    }
    @Nullable
    @Override
    public Task processQuestion(Task task, NAR nar) {
        throw new RuntimeException(shouldntProcess);
    }

    @Nullable
    @Override
    public final Task processQuest(Task task, NAR nar) {
        return processQuestion(task, nar );
    }

    @Nullable
    @Override
    public Task process(Task task, NAR nar) {
        throw new RuntimeException(shouldntProcess);
    }

    /** atoms have no termlink templates, they are irreducible */
    @Override public @Nullable List<Termed> termlinkTemplates() {
        return null;
    }

    /**
     * when a task is processed, a tasklink
     * can be created at the concept of its term
     */
    @Override public boolean link(@NotNull Task task, float scale, float minScale, @NotNull NAR nar) {

        //activate tasklink locally
        Budget taskBudget = task.budget();

        /*if (taskLinkOut(this, t)) {*/
            taskLinks.put(task, taskBudget, scale);
        //}

        return true;
    }

    public final void linkTerm(@NotNull Concept target, Budget b, float subScale) {
        linkTerm(this, target, b, subScale, true, true);
    }

    public static final void linkTerm(@NotNull Concept source, @NotNull Concept target, Budget b, float subScale, boolean out, boolean in) {

        if (source == target)
            throw new RuntimeException("termlink self-loop");

        /** activate local's termlink to template */
        //float termlinkScale = termLinkOut(this, target.term());
        if (out)
            source.termlinks().put(target, b, subScale /* * termlinkScale*/);

        /** activate (reverse) template's termlink to local */
        if (in)
            target.termlinks().put(source, b, subScale);

    }

//    /** filter for inserting an outgoing termlink depending on the target */
//    public static float termLinkOut(Termed from, Term to) {
////        if (!to.isCompound()) {
////            if (from.op().isStatement()) // isAny(Op.ProductOrImageBits)
////                return 0.5f;
////        }
//        return 1f;
//    }
//
//    private static boolean taskLinkOut(@NotNull Concept c, @NotNull Task t) {
//        return true;
//        //return !(c.term().equals(t.term()));
//    }

}
