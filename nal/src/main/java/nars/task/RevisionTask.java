package nars.task;

import nars.bag.Bag;
import nars.budget.BudgetFunctions;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Stamp;
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
public class RevisionTask extends MutableTask  {

    public RevisionTask(@NotNull Termed<Compound> term, @NotNull Task newBelief, Task oldBelief, Truth conclusion, long creationTime, long occTime) {
        super(term, newBelief.punc(), conclusion, newBelief, oldBelief);

        if (!newBelief.isBeliefOrGoal() || !oldBelief.isBeliefOrGoal() )
            throw new UnsupportedOperationException("invalid punctuation");

        time(creationTime, occTime);
        budget(oldBelief, newBelief);
        log("Insertion Revision");
        /*.because("Insertion Revision (%+" +
                        Texts.n2(conclusion.freq() - newBelief.freq()) +
                ";+" + Texts.n2(conclusion.conf() - newBelief.conf()) + "%");*/
    }

    private void budget(Task a, Task b) {
        float acw = a.confWeight();
        float aMix = acw / (acw + b.confWeight());
        budget(a, b, aMix);
    }

    private void budget(Task a, Task b, float aMix) {
        if (!b.isDeleted() && !a.isDeleted()) {
            budget(b.budget());
            BudgetMerge.plusDQBlend.merge(budget(), a.budget(), aMix);
        } else {
            delete();
        }
    }

    public RevisionTask(Compound c, Task a, Task b, long now, long newOcc, float aMix, Truth newTruth) {
        super(c, a, b,
                now, newOcc,
                Stamp.zip(a.evidence(), b.evidence(), aMix),
                newTruth);
        budget(a, b, aMix);
        log("Revection Merge");
    }

//    @Override
//    public boolean isDeleted() {
//        if (super.isDeleted()) {
//            return true;
//        }
//        if (isParentDeleted(getParentTask()) || isParentDeleted(getParentBelief())) {
//            return delete();
//        }
//        return false;
//    }
//    private static boolean isParentDeleted(Task b) {
//        return (b==null || b.isDeleted());
//    }
//

    /** According to the relative improvement in truth quality of the revision, de-prioritize the premise tasks and associated links */
    @Override public boolean onConcept(@NotNull Concept c) {
        super.onConcept(c);

        float resultPri = pri();
//        if (resultPri!=resultPri)
//            return false; //deleted already?

        Task parentNewBelief = getParentTask();
        Task parentOldBelief = getParentBelief();

        if (parentNewBelief==null || parentOldBelief==null) {
            return true; //HACK
        }

        float newBeliefContribution;
        if (parentNewBelief.isBeliefOrGoal()) {
            float newBeliefConf = parentNewBelief.confWeight();
            newBeliefContribution = newBeliefConf / (newBeliefConf + parentOldBelief.confWeight());
        } else {
            //question/quest
            newBeliefContribution = 0.5f;
        }

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

//    private void weaken(Task parent) {
//        if (parent.isDeleted())
//            return;
//
//        //weaken the premise links inversely proportionally to the amount of increase in truth confidence
//        float n = confWeight();
//        float t = parent.confWeight();
//
//        if (n <= t) {
//            if (Global.DEBUG)
//                throw new RuntimeException("Revision failed to increase confidence");
//            return;
//        }
//
//        float factor = n / (n + t);
//
//        //multiplyPremise(factor, true);
//
//        //weaken this task iself
//        Budget b = parent.budget();
//        b.andPriority(factor);
//        b.andDurability(factor);
//
//    }

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
