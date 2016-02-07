package nars.task;

import nars.bag.BLink;
import nars.concept.ConceptProcess;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/7/16.
 */
public final class DerivedTask extends MutableTask {

    public final ConceptProcess premise;

    public DerivedTask(@NotNull Termed<Compound> tc, ConceptProcess premise) {
        super(tc);
        this.premise = premise;
    }

    @Override
    public boolean onRevision(@NotNull Task t) {
        Truth conclusion = t.truth();

        ConceptProcess p = this.premise;

        BLink<? extends Task> tLink = p.taskLink;

        //TODO check this Question case is right
        Truth tLinkTruth = tLink.get().truth();
        if (tLinkTruth != null) {
            float oneMinusDifT = 1f - conclusion.getExpDifAbs(tLinkTruth);
            tLink.andPriority(oneMinusDifT);
            tLink.andDurability(oneMinusDifT);
        }

        Task belief = p.belief();
        if (belief != null) {
            BLink<? extends Termed> bLink = p.termLink;
            float oneMinusDifB = 1f - conclusion.getExpDifAbs(belief.truth());
            bLink.andPriority(oneMinusDifB);
            bLink.andDurability(oneMinusDifB);
        }

        return true;

    }
}
