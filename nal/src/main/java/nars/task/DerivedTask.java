package nars.task;

import nars.Global;
import nars.bag.BLink;
import nars.concept.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;

import static nars.truth.TruthFunctions.c2w;


public final class DerivedTask extends MutableTask {

    @NotNull private final Reference<BLink<? extends Task>> taskLink;
    @NotNull private final Reference<BLink<? extends Termed>> termLink;

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, Truth truth, @NotNull ConceptProcess premise, Reference<Task>[] parents) {
        super(tc, punct, truth, parents);
        this.taskLink = Global.reference(premise.taskLink);
        this.termLink = Global.reference(premise.termLink);
    }

    @Override public void delete() {
        super.delete();
        taskLink.clear();
        termLink.clear();
    }


    @Override
    public boolean onRevision(@NotNull Task next) {
        if (isDeleted())
            return false;

        //weaken the tasklink inversely proportionally to the amount of increase in truth confidence
        float oneMinusDeltaConf = 1f - (c2w(next.conf()) - c2w(this.conf()));
        if (oneMinusDeltaConf >= 1)
            throw new RuntimeException("Revision failed to increase confidence");

        BLink<? extends Task> tLink = taskLink.get();
        if (tLink!=null && !tLink.isDeleted()) {
            tLink.andPriority(oneMinusDeltaConf);
            tLink.andDurability(oneMinusDeltaConf);
        }

        //weaken the termlink in similar way
        BLink<? extends Termed> bLink = termLink.get();
        if (bLink!=null && !bLink.isDeleted()) {
            bLink.andPriority(oneMinusDeltaConf);
            bLink.andDurability(oneMinusDeltaConf);
        }

        return true;
    }

}
