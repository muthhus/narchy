package nars.budget.control;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.util.PriorityAccumulator;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 8/22/16.
 */
public class DepthFirstActivation extends Activation {

    private final int tasklinkDepth;
    private final int termlinkDepth;

    @NotNull
    public final Budgeted in;

    @Nullable public final PriorityAccumulator<Concept> conceptActivation;


    DepthFirstActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, @NotNull NAR nar, int termlinkDepth, int taskLinkDepth, @Nullable PriorityAccumulator<Concept> conceptActivation) {
        super(in, scale, src, nar);
        this.in = in;
        this.conceptActivation = conceptActivation;
        this.termlinkDepth = Math.max(taskLinkDepth, termlinkDepth);  //should be larger then TASKLINK_DEPTH_LIMIT because this resolves the Concept used for it in linkSubterms
        this.tasklinkDepth = taskLinkDepth;
    }

    /**
     * runs the task activation procedure
     */
    public DepthFirstActivation(@NotNull Budgeted in, @NotNull Concept src, @NotNull Concept target, @NotNull NAR nar, float scale, int termlinkDepth, int taskLinkDepth) {
        this(in, scale, src, nar, termlinkDepth, taskLinkDepth, nar.accumulator());

        //self links
        float selfScale = scale / src.templates().size();
        termlink(src, src.term(), selfScale);
        if (in instanceof Task) {
            tasklink(src, (Task)in, selfScale);
        }

        //child links (recurse)
        link(src, target, scale, 0);


        nar.emotion.stress(linkOverflow);
    }

    /**
     * runs the task activation procedure
     */
    public DepthFirstActivation(@NotNull Budgeted in, @NotNull Concept c, @NotNull NAR nar, float scale) {
        this(in, c, c, nar, scale, Param.ACTIVATION_TERMLINK_DEPTH, Param.ACTIVATION_TASKLINK_DEPTH);
    }

    public DepthFirstActivation(@NotNull Task t, @NotNull NAR nar, float scale) {
        this(t, t.concept(nar), nar, scale);
    }


//    public void linkTermLinks(Concept src, float scale) {
//        src.termlinks().forEach(n -> {
//            Term nn = n.get();
//            if (nn!=null)
//                link(src, nn, scale, 0);
//        });
//    }

    /**
     * crosslinks termlinks
     */
    @Nullable
    Concept linkSubterm(@NotNull Termed target, float subScale, int depth) {


        Concept targetConcept = nar.concept(target,
                //dont create a concept if doesnt exist, below this depth
                depth < Param.ACTIVATION_TERMLINK_DEPTH_CONCEPTUALIZE ? true : false
        );


        if (targetConcept != null) {

            //System.out.println("+" + scale + " x " + targetConcept);
            if (conceptActivation!=null)
                conceptActivation.add(targetConcept, subScale);

            if (depth + 1 < termlinkDepth) {

                @NotNull TermContainer templates = targetConcept.templates();
                int n = templates.size();
                if (n > 0) {
                    float subScale1 = /*Param.TERMLINK_TEMPLATE_PRIORITY_FACTOR **/ subScale / n;
                    if (subScale1 >= minScale) { //TODO use a min bound to prevent the iteration ahead of time
                        for (int i = 0; i < n; i++)
                            link(targetConcept, templates.term(i), subScale1, depth + 1); //Link the peer termlink bidirectionally
                    }
                } else {
                    if (Param.ACTIVATE_TERMLINKS_IF_NO_TEMPLATE) {
                        Bag<Term> bbb = targetConcept.termlinks();
                        n = bbb.size();
                        if (n > 0) {
                            float subScale1 = subScale / n;
                            if (subScale1 >= minScale) {
                                bbb.forEachKey(x -> {
                                    //only activate:
                                    linkSubterm(x, subScale1, depth + 1); //Link the peer termlink bidirectionally
                                });
                            }
                        }
                    }
                }

            }

        }


        return targetConcept;
    }

    protected final void link(@NotNull Concept src, @NotNull Termed target, float scale, int depth) {

        if (scale < minScale)
            return;

        Concept targetConcept = linkSubterm(target, scale, depth);

        Term sourceTerm = src.term();
        Term targetTerm = target.term();

        if (!targetTerm.equals(sourceTerm)) {

            // insert termlink target to source
            //float tlFlow = Param.ACTIVATION_TERMLINK_BALANCE;

            /* insert termlink source to target */
            final float tlReverse = scale;
            if (tlReverse >= minScale)
                termlink(src, targetTerm, tlReverse);


            if (targetConcept != null) {
                final float tlForward = scale;
                if (tlForward >= minScale)
                    termlink(targetConcept, sourceTerm, tlForward);
            }

            //System.out.println(src + "<-" + sourceTerm + " -> " + target + "<-" + targetTerm);


            if (targetConcept != null && in instanceof Task && depth <= tasklinkDepth) {
                tasklink(targetConcept, (Task)in, scale);
            }
        }

    }

    protected void tasklink(Concept target, @NotNull Task t, float scale) {
        target.tasklinks().put(t, t, scale, null);
    }

    protected void termlink(Concept from, Term to, float scale) {
        from.termlinks().put(to, in, scale, linkOverflow);
    }



}
