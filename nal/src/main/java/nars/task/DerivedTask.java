package nars.task;

import nars.link.BLink;
import nars.nal.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;


public final class DerivedTask extends MutableTask {

    //if the links are weak then these dont need to be also
    //@NotNull private final Reference<BLink<? extends Task>> taskLink;
    //@NotNull private final Reference<BLink<? extends Termed>> termLink;
    private final @NotNull BLink<? extends Task> taskLink;
    private final @NotNull BLink<? extends Termed> termLink;

    //TODO should this also affect the Belief task?

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, Truth truth, @NotNull ConceptProcess premise, Reference<Task>[] parents) {
        super(tc, punct, truth, parents);
        this.taskLink = (premise.taskLink);
        this.termLink = (premise.termLink);
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

    static void multiply(float factor, @Nullable BLink link, boolean alsoDurability) {
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
