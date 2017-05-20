package nars.task;

import nars.Task;
import nars.term.Compound;
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
public class RevisionTask extends AnswerTask {


    public RevisionTask(@NotNull Compound term, @NotNull Task newBelief, @NotNull Task oldBelief, Truth conclusion, long creationTime, long start, long end) {
        super(term, newBelief, oldBelief, conclusion, creationTime, start, end,
                0.5f /* TODO try proportional stamps */);

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


//    /**
//     * According to the relative improvement in truth quality of the revision, de-prioritize the premise tasks and associated links
//     */
//    @Override
//    public void eval(NAR n) throws Concept.InvalidConceptException, InvalidTermException, InvalidTaskException {
//
//        //TODO reimplement again
//
//        float resultPri = pri();
//        if (resultPri != resultPri) {
//            return;
//        }
//
//
//        float newBeliefContribution;
//        if (parentNewBelief.isBeliefOrGoal()) {
//            //int dur = n.dur();
//            float newBeliefConf = parentNewBelief.evi();
//            newBeliefContribution = newBeliefConf / (newBeliefConf + parentOldBelief.evi());
//        } else {
//            //question/quest
//            newBeliefContribution = 0.5f;
//        }
//
//
//        //Balance Tasks
//        BudgetFunctions.balancePri(
//                parentNewBelief.priority(), parentOldBelief.priority(),
//                resultPri,
//                newBeliefContribution);
//
//        //Balance Tasklinks
//        TaskConcept cn = concept(n);
//        Bag<Task, PLink<Task>> tasklinks = cn.tasklinks();
//        BudgetFunctions.balancePri(
//                tasklinks.get(parentNewBelief), tasklinks.get(parentOldBelief),
//                resultPri,
//                newBeliefContribution);
//
//
//
//    }

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
