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


    //avoid storing the ConceptProcess reference because it creates a garbage-collection chain of derivedtask -> premise -> derivedtask etc..
    //public final ConceptProcess premise;

    public DerivedTask(@NotNull Termed<Compound> tc, char punct, Truth truth, @NotNull ConceptProcess premise) {
        super(tc, punct, truth);
        this.premise = Global.reference(premise);


    }

    @Override public void delete() {
        super.delete();
        premise.clear();
    }


    @Override
    public boolean onRevision(@NotNull Task t) {

        if (isDeleted())
            return false;

        ConceptProcess premise = this.premise.get();
        if (premise == null)
            return true; //weakref may cause these to become null

        Truth conclusion = t.truth();

        BLink<? extends Task> tLink = premise.taskLink;
        if (tLink.isDeleted()) {
            //System.out.println(premise.taskLink + " should delete " + this + "?");

            delete("Premise TaskLink Deleted");


        /*if (Global.DEBUG_DERIVATION_STACKTRACES && Global.DEBUG_TASK_LOG)
            task.log(Premise.getStack());*/

            //eventTaskRemoved.emit(task);

        /* else: a more destructive cleanup of the discarded task? */

            return false;
        } else {
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

        return true;
    }

}
