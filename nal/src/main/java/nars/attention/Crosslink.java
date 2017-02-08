package nars.attention;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/18/16.
 */
public class Crosslink {

    public static void crossLink(@NotNull Task srcTask, @NotNull Task tgtTask, float scale, @NotNull NAR nar) {
        crossLink(srcTask.concept(nar), srcTask, tgtTask, scale, nar);
    }

    /**
     * @param src task with a term equal to this concept's
     * @param tgt task with a term equal to another concept's
     * @return true if the tgt task's concept is different from this Concept, in which case a crossLink has been applied. false otherwise
     */
    public static void crossLink(Concept srcConcept, @NotNull Task srcTask, @NotNull Task tgtTask, float scale, @NotNull NAR nar) {
        Concept tgtConcept = tgtTask.concept(nar);
        if (tgtConcept == null || tgtConcept.term().equals(srcConcept.term()))
            return; //null or same concept


        new SpreadingActivation(srcTask, scale, tgtConcept, 1, nar);
        new SpreadingActivation(tgtTask, scale, srcConcept, 1, nar);
    }
}
