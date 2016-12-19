package nars.budget.control;

import jcog.data.LightObjectFloatPair;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by me on 8/22/16.
 */
public class SpreadingActivation extends Activation {

    private final int termlinkDepth;

    @NotNull public final Budgeted in;

    public final ObjectFloatHashMap<Term> spread = new ObjectFloatHashMap<>();

    final static float parentRetention = 0.5f;

    SpreadingActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, @NotNull NAR nar, int termlinkDepth) {
        super(in, scale, src, nar);
        this.in = in;
        this.termlinkDepth = termlinkDepth;  //should be larger then TASKLINK_DEPTH_LIMIT because this resolves the Concept used for it in linkSubterms
    }


    /**
     * runs the task activation procedure
     */
    public SpreadingActivation(@NotNull Budgeted in, @NotNull Concept c, @NotNull NAR nar, float scale) {
        this(in, scale, c, Param.ACTIVATION_TERMLINK_DEPTH, nar);
    }

    /**
     * unidirectional task activation procedure
     */
    public SpreadingActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, int termlinkDepth, @NotNull NAR nar) {
        this(in, scale, src, nar, termlinkDepth);

        link(src.term(), scale, 0);

        //PriorityAccumulator<Concept> conceptActivation = nar.accumulator();

        //System.out.println(in + ":");


        List<ObjectFloatPair<Concept>> conceptActivation = $.newArrayList(spread.size());

        spread.forEachKeyValue((k, v) -> {
            //System.out.println("\t" + k + " " + v);

            Termed kk = nar.concept(k, true);
            if (kk != null) {
                Concept ckk = (Concept) kk;

                tasklink(ckk, v);

                float conceptActivationRate = 1f;
                conceptActivation.add(new LightObjectFloatPair<>(ckk, v * conceptActivationRate));

            } else {
                kk = k;
            }

            termBidi(kk, v, v);

        });

        nar.activate(conceptActivation, null);

        nar.emotion.stress(linkOverflow);
    }


    @Nullable
    void link(@NotNull Term targetTerm, float scale, int depth) {

        float thisScale = scale;

        if (targetTerm instanceof Compound && ((depth + 1) <= termlinkDepth)) {

            TermContainer children = ((Compound) targetTerm).subterms();
            int n = children.size();

            float childScale = ((1f-parentRetention) * scale) / (n);
            if ( childScale >= minScale) {
                Term[] tt = children.terms();
                for (Term t : tt)
                    link(t.unneg(), childScale, depth + 1); //link and recurse to the concept

                thisScale = scale * parentRetention;
            }
        }

        spread.addToValue(targetTerm, thisScale);

    }

    final void termBidi(@NotNull Termed tgt, float tlForward, float tlReverse) {

        //System.out.println( "\t" + origin + " " + src + " " + tgt + " "+ n2(scale));

        Term tgtTerm = tgt.term();

        if (tlForward > 0)
            termlink(origin, tgtTerm, tlForward);

        if (tgt instanceof Concept) {
            Term originTerm = origin.term();
            if (tlReverse > 0 && !originTerm.equals(tgtTerm)) {
                termlink((Concept) tgt, originTerm, tlReverse);
            }
        }

    }

    void tasklink(Concept target, float scale) {
        Task src = (Task) in;
        target.tasklinks().put(src, src, scale, null);
    }

    void termlink(Concept from, Term to, float scale) {
        from.termlinks().put(to, in, scale, linkOverflow);
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