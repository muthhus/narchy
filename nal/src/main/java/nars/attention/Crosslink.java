package nars.attention;

import jcog.pri.PLink;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/18/16.
 */
public class Crosslink {

//    public static void crossLink(@NotNull Task srcTask, @NotNull Task tgtTask, float scale, @NotNull NAR nar) {
//        crossLink(srcTask.concept(nar), srcTask, tgtTask, scale, nar);
//    }

    /**
     * @param src task with a term equal to this concept's
     * @param tgt task with a term equal to another concept's
     * @return true if the tgt task's concept is different from this Concept, in which case a crossLink has been applied. false otherwise
     */
    public static void crossLink(@NotNull Concept srcConcept, @NotNull Task srcTask, @NotNull Task tgtTask, float scaleSrcTgt, float scaleTgtSrc, @NotNull NAR nar) {

        Concept tgtConcept = tgtTask.concept(nar);
        if (tgtConcept == null || tgtConcept.equals(srcConcept))
            return; //null or same concept

        //termlinks are necessary:
        tgtConcept.termlinks().put( new PLink(srcConcept, scaleSrcTgt));
        srcConcept.termlinks().put( new PLink(tgtConcept, scaleTgtSrc));

        //tasklinks, not sure:
//        tgtConcept.tasklinks().put( new RawPLink(srcTask, scaleSrcTgt));
//        srcConcept.tasklinks().put( new RawPLink(tgtTask, scaleTgtSrc));


    }
}
