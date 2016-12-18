package nars.budget.control;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.Budgeted;
import nars.budget.util.PriorityAccumulator;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
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

    @Nullable
    public final PriorityAccumulator<Concept> conceptActivation;


    DepthFirstActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, @NotNull NAR nar, int termlinkDepth, int taskLinkDepth, @Nullable PriorityAccumulator<Concept> conceptActivation) {
        super(in, scale, src, nar);
        this.in = in;
        this.conceptActivation = conceptActivation;
        this.termlinkDepth = termlinkDepth;  //should be larger then TASKLINK_DEPTH_LIMIT because this resolves the Concept used for it in linkSubterms
        this.tasklinkDepth = taskLinkDepth;
    }

    /**
     * unidirectional task activation procedure
     */
    public DepthFirstActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, int termlinkDepth, int taskLinkDepth, @NotNull NAR nar) {
        this(in, scale, src, nar, termlinkDepth, taskLinkDepth, nar.accumulator());

        link(src, scale, 0);

        nar.emotion.stress(linkOverflow);
    }


    /**
     * runs the task activation procedure
     */
    public DepthFirstActivation(@NotNull Budgeted in, @NotNull Concept c, @NotNull NAR nar, float scale) {
        this(in, scale, c, Param.ACTIVATION_TERMLINK_DEPTH, Param.ACTIVATION_TASKLINK_DEPTH, nar);
    }


    @Nullable
    void link(@NotNull Concept targetConcept, float subScale, int depth) {

        if (in instanceof Task && depth <= tasklinkDepth)
            tasklink((Task) in, targetConcept, subScale);

        if (depth <= termlinkDepth)
            termlink(targetConcept, subScale, depth);
    }

    protected void termlink(@NotNull Concept targetConcept, float subScale, int depth) {

        termCrosslink(targetConcept, subScale);

        if (conceptActivation != null)
            conceptActivation.add(targetConcept, subScale);

        if (!(targetConcept.term() instanceof Compound))
            return;

        @NotNull TermContainer templates =
            ((Compound)targetConcept.term()).subterms();

        int n = templates.size();

        float tlScale = /*Param.TERMLINK_TEMPLATE_PRIORITY_FACTOR **/ subScale / (n);
        if (tlScale >= minScale) { //TODO use a min bound to prevent the iteration ahead of time

            boolean activateTemplate = (depth + 1) < Param.ACTIVATION_CONCEPTUALIZE_DEPTH ? true : false;
            for (int i = 0; i < n; i++) {

                Term tt = templates.term(i).unneg();
                Concept tc = nar.concept(tt, activateTemplate);
                if (tc != null) {
                    link(tc, tlScale, depth + 1); //link and recurse to the concept
                } else {
                    termlink(origin, tt, tlScale); //just link to the term
                }
            }

        }
    }

    protected final void termCrosslink(@NotNull Termed tgt, float scale) {
        termCrosslink(tgt, scale, scale);
    }

    protected final void termCrosslink(@NotNull Termed tgt, float tlForward, float tlReverse) {

        //System.out.println( "\t" + origin + " " + src + " " + tgt + " "+ n2(scale));

        Term srcTerm = origin.term();
        Term tgtTerm = tgt.term();


        /* insert termlink source to target */
        if (tlForward > 0)
            termlink(origin, tgtTerm, tlForward);

        if (tgt instanceof Concept && !srcTerm.equals(tgtTerm) && tlReverse > 0) {
            termlink((Concept)tgt, srcTerm, tlReverse);
        }

    }

    protected void tasklink(@NotNull Task src, Concept target, float scale) {
        target.tasklinks().put(src, src, scale, null);
    }

    protected void termlink(Concept from, Term to, float scale) {
        from.termlinks().put(to, in, scale, linkOverflow);
    }


    public static class SpreadingActivation extends DepthFirstActivation {

        private ObjectFloatHashMap<Term> spread;
        boolean post = false;

        public SpreadingActivation(@NotNull Budgeted in, @NotNull Concept c, @NotNull NAR nar, float scale) {
            this(in, scale, c, Param.ACTIVATION_TERMLINK_DEPTH, Param.ACTIVATION_TASKLINK_DEPTH, nar);
        }

        public SpreadingActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, int termlinkDepth, int taskLinkDepth, @NotNull NAR nar) {
            super(in, scale, src, termlinkDepth, taskLinkDepth, nar);

            if (spread!=null) {
                post = true;

                //System.out.println(in + ":");
                spread.forEachKeyValue((k, v) -> {
                    //System.out.println("\t" + k + " " + v);

                    Termed kk = nar.concept(k /* TODO conceptualize? */);
                    if (kk==null) {
                        kk = k;
                    } else {
                        super.tasklink((Task) in, (Concept) kk, v);
                    }
                    super.termCrosslink(kk, v);
                });
            }
        }

        @Override
        @Nullable void link(@NotNull Concept targetConcept, float subScale, int depth) {
            if (depth == 0)
                spread = new ObjectFloatHashMap<>(); //HACK

            super.link(targetConcept, subScale, depth);
        }

        @Override
        protected void tasklink(@NotNull Task src, Concept target, float scale) {

        }

        protected void termlink(@NotNull Concept src, Term target, float scale) {


                if (!post) {
                    //FloatToFloatFunction updater = (v) -> Math.max(v, scale);
                    //spread.updateValue(targett, 0, updater);
//                    Term targetConcept = nar.concepts.conceptualizable(target, true);
//                    if (targetConcept == null)
//                        targetConcept = target;

                    spread.addToValue(target, scale);

                } else {
                    super.termlink(src, target, scale);
                }


        }

    }


}


            /*else {
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
            }*/