package nars.task;

import nars.Global;
import nars.bag.BLink;
import nars.concept.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;

/**
 * Created by me on 2/7/16.
 */
public final class DerivedTask extends MutableTask {

    @Nullable
    private final Reference<ConceptProcess> premise;
    @Nullable
    private BLink<? extends Task> premiseTaskLink;
    @Nullable
    private BLink<? extends Termed> premiseTermLink;

    //avoid storing the ConceptProcess reference because it creates a garbage-collection chain of derivedtask -> premise -> derivedtask etc..
    //public final ConceptProcess premise;

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, @NotNull ConceptProcess premise) {
        super(tc, punct);
        this.premise = Global.reference(premise);


    }

    @Override public void delete() {
        super.delete();
        premise.clear();
    }


    @Override
    public void onRevision(@NotNull Task t) {
        if (isDeleted())
            return;

        ConceptProcess premise = this.premise.get();
        if (premise == null)
            return; //weakref may cause these to become null

        Truth conclusion = t.truth();

        BLink<? extends Task> tLink = premise.taskLink;
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
            BLink<? extends Termed> bLink = premise.termLink;
            if (!bLink.isDeleted()) {
                float oneMinusDifB = 1f - conclusion.getExpDifAbs(belief.truth());
                bLink.andPriority(oneMinusDifB);
                bLink.andDurability(oneMinusDifB);
            }
        }

    }

}
