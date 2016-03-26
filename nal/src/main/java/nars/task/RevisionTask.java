package nars.task;

import nars.bag.BLink;
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

    public RevisionTask(@NotNull Termed<Compound> term, Budget revisionBudget, @NotNull Task newBelief, Task oldBelief, Truth conclusion, long now, long concTime) {
        super(term, newBelief.punc());

        budget(revisionBudget);
        truth(conclusion);
        parent(newBelief, oldBelief);
        time(now, concTime);
        because("Insertion Revision");
        /*.because("Insertion Revision (%+" +
                        Texts.n2(conclusion.freq() - newBelief.freq()) +
                ";+" + Texts.n2(conclusion.conf() - newBelief.conf()) + "%");*/
    }

    @Override
    public void onConcept(@NotNull Concept c) {
        super.onConcept(c);

        Task newBelief = getParentTask();
        Task oldBelief = getParentBelief();

        if ((newBelief == null) || (oldBelief == null))
            return; //weakref may cause these to become null

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


        oldBelief.onRevision(this);
        newBelief.onRevision(this);

    }

}
