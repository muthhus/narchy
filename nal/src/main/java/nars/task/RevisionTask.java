package nars.task;

import jcog.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.budget.BLink;
import nars.budget.BudgetFunctions;
import nars.concept.TaskConcept;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The result of belief/goal revision. Also responsible for balancing
 * budget between its 2 parent tasks, the child revision task (this),
 * and their tasklinks.
 * The budget flows from the parents if and when the revision task
 * is processed, not on construction.
 * This is in case the revision task for some reason does not become processed,
 * then this budget will not be moved.
 */
public class RevisionTask extends AnswerTask  {


    @Nullable
    private TaskConcept concept;

    public RevisionTask(@NotNull Compound term, @NotNull Task newBelief, @NotNull Task oldBelief, Truth conclusion, long creationTime, long start, long end, TaskConcept target) {
        super(term, newBelief, oldBelief, conclusion, creationTime, start, end, 0.5f);
        this.concept = target;
    }


    public RevisionTask(@NotNull Compound term, byte punc, Truth conclusion, long creationTime, long start, long end, long[] evidence) {
        super(term, punc, conclusion, creationTime, start, end, evidence);
    }

//    public RevisionTask(@NotNull Compound c, @NotNull Task a, @NotNull Task b, long now, long newOcc, float aMix, Truth newTruth, Concept target) {
//        super(c, a, b, newTruth, now, newOcc, aMix);
//
//        if (!a.isBeliefOrGoal() || !b.isBeliefOrGoal() )
//            throw new UnsupportedOperationException("invalid punctuation");
//
//        this.concept = target;
//    }

    @Override
    public @Nullable TaskConcept concept(@NotNull NAR n) {
        if (concept==null)
            return super.concept(n); //HACK
        return concept;
    }

    @Override
    public void unlink() {
        super.unlink();
        concept = null;
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
    @Override
    public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {

        //TODO reimplement again

        float resultPri = pri();
        if (resultPri!=resultPri) {
            unlink();
            return;
        }

        Task parentNewBelief = getParentTask();
        if (parentNewBelief==null) {
            unlink();
            return; //HACK
        }

        Task parentOldBelief = getParentBelief();
        if (parentOldBelief==null) {
            unlink();
            return; //HACK
        }

        float newBeliefContribution;
        if (parentNewBelief.isBeliefOrGoal()) {
            int dur = nar.dur();
            float newBeliefConf = parentNewBelief.evi(dur);
            newBeliefContribution = newBeliefConf / (newBeliefConf + parentOldBelief.evi(dur));
        } else {
            //question/quest
            newBeliefContribution = 0.5f;
        }

        try {
            //Balance Tasks
            BudgetFunctions.balancePri(
                    parentNewBelief.budget(), parentOldBelief.budget(),
                    resultPri,
                    newBeliefContribution);

            //Balance Tasklinks
            Bag<Task,BLink<Task>> tasklinks = concept(nar).tasklinks();
            BudgetFunctions.balancePri(
                    tasklinks.get(parentNewBelief), tasklinks.get(parentOldBelief),
                    resultPri,
                    newBeliefContribution);

        } catch (BudgetException ignored) {
            //HACK
        }

        unlink();
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
//        Bag<Task,BLink<Task>> tasklinks = c.tasklinks();
//        BudgetFunctions.balancePri(
//                tasklinks.get(newBelief), tasklinks.get(oldBelief),
//                resultPri,
//                newBeliefContribution);
