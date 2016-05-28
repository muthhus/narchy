package nars.task;

import nars.bag.BLink;
import nars.nal.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;


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

    @Override public boolean delete() {
        if (super.delete()) {
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

//            float decayFactor = 1f - ((isBeliefOrGoal() ? conf() : qua() ) *durability);
//            multiplyPremise(decayFactor, false);

            return true;
        }

        return false;
    }

//    /** next = the child which resulted from this and another task being revised */
//    @Override public boolean onRevision(@NotNull Task next) {
//
//
//        return true;
//    }

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
