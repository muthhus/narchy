package nars.task;

import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * The result of belief/goal revision. Also responsible for balancing
 * budget between its 2 parent tasks, the child revision task (this),
 * and their tasklinks.
 * The budget flows from the parents if and when the revision task
 * is processed, not on construction.
 * This is in case the revision task for some reason does not become processed,
 * then this budget will not be moved.
 */
public class RevisionTask extends MutableTask {

    public RevisionTask(@NotNull Termed<Compound> term, Budget revisionBudget, @NotNull Task newBelief, Task oldBelief, Truth conclusion, long creationTime, long occTime) {
        super(term, newBelief.punc(), conclusion, newBelief, oldBelief);

        budget(revisionBudget);
        time(creationTime, occTime);
        because("Insertion Revision");
        /*.because("Insertion Revision (%+" +
                        Texts.n2(conclusion.freq() - newBelief.freq()) +
                ";+" + Texts.n2(conclusion.conf() - newBelief.conf()) + "%");*/
    }

//    public RevisionTask(@NotNull Termed<Compound> newTerm, @NotNull Task taskToClone, @NotNull Task otherTask, long now, long occ, long[] newEvidence, Truth newTruth, @NotNull BudgetMerge budgetMerge) {
//        super(newTerm, taskToClone, otherTask, now, occ, newEvidence, newTruth, budgetMerge);
//    }

    @Override
    public boolean onConcept(@NotNull Concept c) {
        super.onConcept(c);

        Task newBelief = getParentTask();
        Task oldBelief = getParentBelief();

        if ((newBelief == null) || (oldBelief == null))
            return true; //weakref may cause these to become null; so just continue processing


        //Decrease the budget of the parent tasks and tasklinks,
        // so that their priority sum and the child remains the same (balanced)
        //TODO maybe consider rank (incl. evidence) not just conf()
        float newBeliefConf = newBelief.conf();
        float newBeliefContribution = newBeliefConf / (newBeliefConf + oldBelief.conf());
        //oldBeliefContribution = 1 - newBeliefContribution, summing to 1


        float resultPri = pri();

        //Balance Tasks
        BudgetFunctions.balancePri(
                newBelief.budget(), oldBelief.budget(),
                resultPri,
                newBeliefContribution);

        //Balance Tasklinks
        Bag<Task> tasklinks = c.tasklinks();
        BudgetFunctions.balancePri(
                tasklinks.get(newBelief), tasklinks.get(oldBelief),
                resultPri,
                newBeliefContribution);


        boolean oldExists = oldBelief.onRevision(this);
        boolean newExists = newBelief.onRevision(this);

        return true;

        //return oldExists || newExists;
        //return oldBelief.onRevision(this) && newBelief.onRevision(this)

    }

}
