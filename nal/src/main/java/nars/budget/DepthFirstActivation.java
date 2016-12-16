package nars.budget;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
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


    public DepthFirstActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, @NotNull NAR nar, int termlinkDepth, int taskLinkDepth, @Nullable PriorityAccumulator<Concept> conceptActivation) {
        super(nar, in, scale, src);
        this.in = in;
        this.conceptActivation = conceptActivation;
        this.termlinkDepth = Math.max(taskLinkDepth, termlinkDepth);  //should be larger then TASKLINK_DEPTH_LIMIT because this resolves the Concept used for it in linkSubterms
        this.tasklinkDepth = taskLinkDepth;
    }

    /**
     * runs the task activation procedure
     */
    public DepthFirstActivation(@NotNull Budgeted in, @NotNull Concept src, @NotNull Concept target, @NotNull NAR nar, float scale, int termlinkDepth, int taskLinkDepth, PriorityAccumulator<Concept> conceptActivation) {
        this(in, scale, src, nar, termlinkDepth, taskLinkDepth, conceptActivation);
        link(src, target, scale, 0);
    }

    /**
     * runs the task activation procedure
     */
    public DepthFirstActivation(@NotNull Budgeted in, @NotNull Concept c, @NotNull NAR nar, float scale, PriorityAccumulator<Concept> conceptActivation) {
        this(in, c, c, nar, scale, Param.ACTIVATION_TERMLINK_DEPTH, Param.ACTIVATION_TASKLINK_DEPTH, conceptActivation);
    }

    public DepthFirstActivation(@NotNull Task in, @NotNull NAR nar, float scale, PriorityAccumulator<Concept> conceptActivation) {
        this(in, in.concept(nar), nar, scale, conceptActivation);
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

        if (canSelfTermlink(sourceTerm) && targetTerm.equals(sourceTerm)) {

            //self termlink
            src.termlinks().put(targetTerm, in, scale, linkOverflow);

        } else {

            // insert termlink target to source
            float tlFlow = Param.ACTIVATION_TERMLINK_BALANCE;

            if (targetConcept != null) {

                final float tlForward = scale * tlFlow;
                if (tlForward >= minScale)
                    targetConcept.termlinks().put(sourceTerm, in, tlForward, linkOverflow);
            }

            /* insert termlink source to target */
            final float tlReverse = scale * (1f - tlFlow);
            if (tlReverse >= minScale)
                src.termlinks().put(targetTerm, in, tlReverse, linkOverflow);

            //System.out.println(src + "<-" + sourceTerm + " -> " + target + "<-" + targetTerm);
        }


        if (targetConcept != null && depth <= tasklinkDepth && in instanceof Task) {
            targetConcept.tasklinks().put((Task) in, in, scale, null);
        }


    }

    private boolean canSelfTermlink(Term sourceTerm) {

        if (sourceTerm instanceof Atomic)
            return false;

        Op o = src.op();
        return !o.image;
    }


}
