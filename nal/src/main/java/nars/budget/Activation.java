package nars.budget;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 8/22/16.
 */
public class Activation {

    private final int tasklinkDepth;
    private final int termlinkDepth;

    public final Budgeted in;

    public final Concept src;

    public final ObjectFloatHashMap<Concept> concepts = new ObjectFloatHashMap<>();
    public final MutableFloat linkOverflow = new MutableFloat(0);
    public final MutableFloat conceptOverflow = new MutableFloat(0);
    private final NAR nar;
    private final float minScale; //cut-off limit for recursive spread


    protected Activation(Budgeted in, Concept src, NAR nar, int termlinkDepth, int taskLinkDepth) {
        this.nar = nar;
        this.in = in;
        this.src = src;
        this.minScale = Param.BUDGET_EPSILON / in.pri();
        this.termlinkDepth = Math.max(taskLinkDepth, termlinkDepth);  //should be larger then TASKLINK_DEPTH_LIMIT because this resolves the Concept used for it in linkSubterms
        this.tasklinkDepth = taskLinkDepth;
    }

    /**
     * runs the task activation procedure
     */
    public Activation(Budgeted in, Concept src, Concept target, NAR nar, float scale, int termlinkDepth, int taskLinkDepth) {
        this(in, src, nar, termlinkDepth, taskLinkDepth);

        if (scale >= minScale) {
            link(src, target, scale, 0);
            commit(scale); //values will already be scaled
        }
    }

    /**
     * runs the task activation procedure
     */
    public Activation(Budgeted in, Concept c, NAR nar, float scale) {
        this(in, c, c, nar, scale, 1, 1);
    }




//    public void linkTermLinks(Concept src, float scale) {
//        src.termlinks().forEach(n -> {
//            Term nn = n.get();
//            if (nn!=null)
//                link(src, nn, scale, 0);
//        });
//    }

    void linkTerms(@NotNull Concept src, @NotNull Term[] tgt, float scale, int depth) {

        int n = tgt.length;
        float subScale = scale / n;

        if (subScale >= minScale) { //TODO use a min bound to prevent the iteration ahead of time

            //then link this to terms
            for (int i = 0; i < n; i++)
                link(src, tgt[i], subScale, depth); //Link the peer termlink bidirectionally
        }
    }

    /**
     * crosslinks termlinks
     */
    @Nullable
    Concept linkSubterm(@NotNull Concept source, @NotNull Termed target, float subScale, int depth) {

    /* activate concept */
        Concept targetConcept = nar.concept(target, true);
        Term targetTerm;

        if (targetConcept!=null) {
            activateConcept(targetConcept, subScale);

            if (depth < termlinkDepth) {
                @NotNull TermContainer ttt = targetConcept.templates();
                if (ttt.size() > 0) {
                    linkTerms(targetConcept, ttt.terms(), subScale, depth + 1);
                }
            }

            targetTerm = targetConcept.term();
        } else {
            targetTerm = target.term();
        }

        Term sourceTerm = source.term();
        if (!targetTerm.equals(sourceTerm)) {

            /* insert termlink target to source */
            //boolean alsoReverse = true;
            if (targetConcept != null /*&& alsoReverse*/) {
                targetConcept.termlinks().put(sourceTerm, in, subScale, linkOverflow);
            }

            /* insert termlink source to target */
            source.termlinks().put(targetTerm, in, subScale, linkOverflow);
        }

        return targetConcept;
    }

    protected final void link(Concept src, Termed target, float scale, int depth) {

        Concept targetConcept = linkSubterm(src, target, scale, depth);;

        if (targetConcept != null && depth <= tasklinkDepth && in instanceof Task) {
            targetConcept.tasklinks().put((Task) in, in, scale, null);
        }

    }


    public void commit(float scale) {
        if (!concepts.isEmpty()) {
            concepts.compact();
            nar.activate(concepts, in,
                    scale * (float)concepts.sum(),
                    conceptOverflow);
        }
    }

    public void activateConcept(Concept targetConcept, float scale) {
        //System.out.println("+" + scale + " x " + targetConcept);
        concepts.addToValue(targetConcept, scale);
    }

}
