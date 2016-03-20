package nars.task;

import nars.Memory;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;

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

    public RevisionTask(Termed<Compound> term, Budget revisionBudget, Task newBelief, Task oldBelief, Truth conclusion, long now, long concTime) {
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
    public void onConcept(Concept c) {
        super.onConcept(c);

        Task newBelief = getParentTask();
        Task oldBelief = getParentBelief();

        oldBelief.onRevision(this);
        newBelief.onRevision(this);

        float newBeliefConf = newBelief.conf();

        //decrease the budget of the parents so the priority sum among the 2 parents and the child remains the same (balanced)
        //TODO maybe consider rank (incl. evidence) not just conf()
        float newBeliefContribution = newBeliefConf / (newBeliefConf + oldBelief.conf());
        float oldBeliefContribution = 1f - newBeliefContribution;
        float revisionPri = pri();
        float newDiscount = revisionPri * oldBeliefContribution;
        float oldDiscount = revisionPri * newBeliefContribution;


        float nextNewPri = newBelief.pri() - newDiscount;
        float nextOldPri = oldBelief.pri() - oldDiscount;

        if (nextNewPri < 0) {
            nextOldPri -= -nextNewPri; //subtract remainder from the other
            nextNewPri = 0;
        }
        if (nextOldPri < 0) {
            nextNewPri -= -nextOldPri; //subtract remainder from the other
            nextOldPri = 0;
        }

        assert(!((nextNewPri < 0) || (nextOldPri < 0))); //throw new RuntimeException("revision budget underflow");


        //apply the changes
        newBelief.budget().setPriority(nextNewPri);
        oldBelief.budget().setPriority(nextOldPri);

    }

}
