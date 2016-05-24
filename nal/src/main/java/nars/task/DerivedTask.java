package nars.task;

import nars.Global;
import nars.bag.BLink;
import nars.concept.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.ref.Reference;

import static nars.truth.TruthFunctions.c2w;


public final class DerivedTask extends MutableTask {

    //if the links are weak then these dont need to be also
    //@NotNull private final Reference<BLink<? extends Task>> taskLink;
    //@NotNull private final Reference<BLink<? extends Termed>> termLink;
    @NotNull private final BLink<? extends Task> taskLink;
    @NotNull private final BLink<? extends Termed> termLink;

    //TODO should this also affect the Belief task?

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, Truth truth, @NotNull ConceptProcess premise, Reference<Task>[] parents) {
        super(tc, punct, truth, parents);
        this.taskLink = (premise.taskLink);
        this.termLink = (premise.termLink);
    }

    @Override public void delete() {
        if (!isDeleted()) {
            //weaken the premise components which formed this in proportion to the confidence.
            //ie, the weaker the conf the less budget penalty because these are more likely and frequently to be removed

            //this produces negative feedback anytime a derived task is deleted, for example,
            //if a duplicate already existed. then the premise is less likely to be activated
            //in the future having been attenuated.

//            if (isBeliefOrGoal()) {
//
//
//                //more confidence involved makes the penalization more severe
//                //more evidence involved makes the penalization less severe?
//
//                //float c = conf();
//
//                //taskLink.clear();
//                //termLink.clear();
//            } else {
//                //TODO for Questions?
//            }

//            float deathFactor = 0.5f; //1f - c;
//            multiplyPremise(deathFactor, false);

            super.delete();
        }
    }

    /** next = the child which resulted from this and another task being revised */
    @Override public boolean onRevision(@NotNull Task next) {
        if (isDeleted())
            return false;

        //weaken the premise links inversely proportionally to the amount of increase in truth confidence
        float n = next.confWeight();
        float t = this.confWeight();

        if (n <= t) {
            if (Global.DEBUG)
                throw new RuntimeException("Revision failed to increase confidence");
            return false;
        }

        float factor = n / (n + t);

        multiplyPremise(factor, true);

        //weaken this task iself
        andPriority(factor);
        andDurability(factor);

        return true;
    }

    public void multiplyPremise(float factor, boolean alsoDurability) {
        multiply(factor, taskLink, alsoDurability);
        multiply(factor, termLink, alsoDurability);
    }

    static void multiply(float factor, BLink link, boolean alsoDurability) {
        if (link !=null && !link.isDeleted()) {
            link.andPriority(factor);
            if (alsoDurability)
                link.andDurability(factor);
        }
    }


}
//scratch
//float deathFactor = 1f - 1f / (1f +(conf()/evidence().length));
//float deathFactor = (1f/(1 + c * c * c * c));
