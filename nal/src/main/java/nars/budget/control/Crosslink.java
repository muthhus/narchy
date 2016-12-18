package nars.budget.control;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/18/16.
 */
public class Crosslink {
    /**
     * @param src task with a term equal to this concept's
     * @param tgt task with a term equal to another concept's
     * @return true if the tgt task's concept is different from this Concept, in which case a crossLink has been applied. false otherwise
     */
    public static void crossLink(Concept srcConcept, @NotNull Task srcTask, @NotNull Task tgtTask, float scale, @NotNull NAR nar) {
        Concept tgtConcept = tgtTask.concept(nar);
        if (tgtConcept == null || tgtConcept.term().equals(srcConcept.term()))
            return; //null or same concept

        //termlink=0, tasklink=-1
        new DepthFirstActivation(srcTask, scale * srcTask.conf(), tgtConcept, 0, -1, nar);
        new DepthFirstActivation(tgtTask, scale * tgtTask.conf(), srcConcept, 0, -1, nar);
    }
}
