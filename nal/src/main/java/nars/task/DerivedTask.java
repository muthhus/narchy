package nars.task;

import nars.bag.BLink;
import nars.concept.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 2/7/16.
 */
public final class DerivedTask extends MutableTask {

    @Nullable
    private BLink<? extends Task> premiseTaskLink;
    @Nullable
    private BLink<? extends Termed> premiseTermLink;

    //avoid storing the ConceptProcess reference because it creates a garbage-collection chain of derivedtask -> premise -> derivedtask etc..
    //public final ConceptProcess premise;

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, @NotNull ConceptProcess premise) {
        super(tc, punct);
        //this.premise = premise;
        this.premiseTaskLink = premise.taskLink;
        this.premiseTermLink = premise.termLink;

    }

    @Override public void delete() {
        super.delete();
        premiseTaskLink = null; //clear GC paths
        premiseTermLink = null; //clear GC paths
    }


    @Override
    public void onRevision(@NotNull Task t) {
        if (isDeleted())
            return;

        Truth conclusion = t.truth();

        BLink<? extends Task> tLink = premiseTaskLink;
        if (!tLink.isDeleted()) {
            //TODO check this Question case is right
            Truth tLinkTruth = tLink.get().truth();
            if (tLinkTruth != null) {
                float oneMinusDifT = 1f - conclusion.getExpDifAbs(tLinkTruth);
                tLink.andPriority(oneMinusDifT);
                tLink.andDurability(oneMinusDifT);
            }
        }

        Task belief = t.getParentBelief();
        if (belief != null) {
            BLink<? extends Termed> bLink = premiseTermLink;
            float oneMinusDifB = 1f - conclusion.getExpDifAbs(belief.truth());
            bLink.andPriority(oneMinusDifB);
            bLink.andDurability(oneMinusDifB);
        }

    }

}
