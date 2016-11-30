package nars.budget;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
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

    @NotNull
    public final Budgeted in;

    @NotNull
    public final Concept src;

    public final ObjectFloatHashMap<Concept> concepts = new ObjectFloatHashMap<>();
    public final MutableFloat linkOverflow = new MutableFloat(0);
    public final MutableFloat conceptOverflow = new MutableFloat(0);
    @NotNull
    private final NAR nar;
    private final float minScale; //cut-off limit for recursive spread


    public Activation(@NotNull Budgeted in, float scale, @NotNull Concept src, @NotNull NAR nar, int termlinkDepth, int taskLinkDepth) {
        this.nar = nar;
        this.in = in;
        this.src = src;
        this.minScale = Param.BUDGET_EPSILON / (scale * in.pri());
        this.termlinkDepth = Math.max(taskLinkDepth, termlinkDepth);  //should be larger then TASKLINK_DEPTH_LIMIT because this resolves the Concept used for it in linkSubterms
        this.tasklinkDepth = taskLinkDepth;
    }

    /**
     * runs the task activation procedure
     */
    public Activation(@NotNull Budgeted in, @NotNull Concept src, @NotNull Concept target, @NotNull NAR nar, float scale, int termlinkDepth, int taskLinkDepth) {
        this(in, scale, src, nar, termlinkDepth, taskLinkDepth);

        if (scale >= minScale) {
            link(src, target, scale, 0);

            if (!concepts.isEmpty()) {
                //concepts.compact();
                this.nar.activationAdd(concepts, this.in,
                        scale,
                        conceptOverflow);
            }
        }
    }

    /**
     * runs the task activation procedure
     */
    public Activation(@NotNull Budgeted in, @NotNull Concept c, @NotNull NAR nar, float scale) {
        this(in, c, c, nar, scale, Param.ACTIVATION_TERMLINK_DEPTH, Param.ACTIVATION_TASKLINK_DEPTH);
    }

    public Activation(@NotNull Task in, @NotNull NAR nar, float scale) {
        this(in, in.concept(nar), nar, scale);
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

    /* activate concept */
        Concept targetConcept = nar.concept(target, true);

        if (targetConcept != null) {

            //System.out.println("+" + scale + " x " + targetConcept);
            concepts.addToValue(targetConcept, subScale);

            if (depth < termlinkDepth) {

                @NotNull TermContainer ttt = targetConcept.templates();
                int n = ttt.size();
                if (n > 0) {
                    float subScale1 = subScale / n;
                    if (subScale1 >= minScale) { //TODO use a min bound to prevent the iteration ahead of time
                        for (int i = 0; i < n; i++)
                            link(targetConcept, ttt.term(i), subScale1, depth + 1); //Link the peer termlink bidirectionally
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

        Concept targetConcept = linkSubterm(target, scale, depth);

        Term sourceTerm = src.term();
        Term targetTerm = target.term();

        if (!targetTerm.equals(sourceTerm)) {

//            Budget a;
//            if (linkOverflow.floatValue() > Param.BUDGET_EPSILON) {
//                a = new RawBudget(in.budget());
//                float remainingPri = 1f - a.pri();
//                float taken = Math.min(linkOverflow.floatValue(), remainingPri);
//                linkOverflow.subtract(taken);
//                a.priAdd(taken);
//            } else {
//                a = in.budget(); //unaffected
//            }

            /* insert termlink target to source */
            //boolean alsoReverse = true;
            if (targetConcept != null /*&& alsoReverse*/) {
                targetConcept.termlinks().put(sourceTerm, in, scale/2f, linkOverflow);
            }

            /* insert termlink source to target */
            src.termlinks().put(targetTerm, in, scale/2f, linkOverflow);

        }

        if (targetConcept != null && depth <= tasklinkDepth && in instanceof Task) {
            targetConcept.tasklinks().put((Task) in, in, scale, null);
        }

    }


}
