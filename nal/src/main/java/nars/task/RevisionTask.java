package nars.task;

import nars.Global;
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

    public RevisionTask(@NotNull Termed<Compound> term, @NotNull Task newBelief, Task oldBelief, Truth conclusion, long creationTime, long occTime) {
        super(term, newBelief.punc(), conclusion, newBelief, oldBelief);

        time(creationTime, occTime);
        because("Insertion Revision");
        /*.because("Insertion Revision (%+" +
                        Texts.n2(conclusion.freq() - newBelief.freq()) +
                ";+" + Texts.n2(conclusion.conf() - newBelief.conf()) + "%");*/
    }


    /** According to the relative improvement in truth quality of the revision, de-prioritize the premise tasks and associated links */
    @Override public boolean onConcept(@NotNull Concept c) {
        super.onConcept(c);

        float resultPri = pri();
        Task parentNewBelief = getParentTask();
        Task parentOldBelief = getParentBelief();
        float newBeliefConf = parentNewBelief.confWeight();
        float newBeliefContribution = newBeliefConf / (newBeliefConf + parentOldBelief.confWeight());

        //Balance Tasks
        BudgetFunctions.balancePri(
                parentNewBelief.budget(), parentOldBelief.budget(),
                resultPri,
                newBeliefContribution);

        //Balance Tasklinks
        Bag<Task> tasklinks = c.tasklinks();
        BudgetFunctions.balancePri(
                tasklinks.get(parentNewBelief), tasklinks.get(parentOldBelief),
                resultPri,
                newBeliefContribution);


//        if (parentNewBelief!=null)
//            weaken(parentNewBelief);
//            //parentNewBelief.onRevision(this);
//
//        if (parentOldBelief!=null)
//            weaken(parentOldBelief);
//            //oldBelief.onRevision(this);

        return true;
    }

    private void weaken(Task parent) {
        if (parent.isDeleted())
            return;

        //weaken the premise links inversely proportionally to the amount of increase in truth confidence
        float n = confWeight();
        float t = parent.confWeight();

        if (n <= t) {
            if (Global.DEBUG)
                throw new RuntimeException("Revision failed to increase confidence");
            return;
        }

        float factor = n / (n + t);

        //multiplyPremise(factor, true);

        //weaken this task iself
        Budget b = parent.budget();
        b.andPriority(factor);
        b.andDurability(factor);

    }

}

//        if ((newBelief == null) || (oldBelief == null))
//            return true; //weakref may cause these to become null; so just continue processing
//
//
//        //Decrease the budget of the parent tasks and tasklinks,
//        // so that their priority sum and the child remains the same (balanced)
//        //TODO maybe consider rank (incl. evidence) not just conf()
//        float newBeliefConf = newBelief.conf();
//        float newBeliefContribution = newBeliefConf / (newBeliefConf + oldBelief.conf());
//        //oldBeliefContribution = 1 - newBeliefContribution, summing to 1
//
//
//        float resultPri = pri();
//
//        //Balance Tasks
//        BudgetFunctions.balancePri(
//                newBelief.budget(), oldBelief.budget(),
//                resultPri,
//                newBeliefContribution);
//
//        //Balance Tasklinks
//        Bag<Task> tasklinks = c.tasklinks();
//        BudgetFunctions.balancePri(
//                tasklinks.get(newBelief), tasklinks.get(oldBelief),
//                resultPri,
//                newBeliefContribution);
