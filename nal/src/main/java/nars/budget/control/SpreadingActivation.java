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
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * activation from a point source to its subterm components (termlink templates)
 */
public class SpreadingActivation extends Activation implements ObjectFloatProcedure<Term> {

    private final int termlinkDepth;

    @NotNull public final Budgeted in;

    public final ObjectFloatHashMap<Term> spread;
    final List<ObjectFloatPair<Concept>> conceptActivation;

    final static float parentRetention = 0.5f;

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
        super(in, scale, src, nar);
        this.in = in;
        this.termlinkDepth = termlinkDepth;  //should be larger then TASKLINK_DEPTH_LIMIT because this resolves the Concept used for it in linkSubterms

        Term srcTerm = src.term();

        spread = new ObjectFloatHashMap<>(srcTerm.volume());

        link(srcTerm, scale, 0);

        conceptActivation = $.newArrayList(spread.size());

        spread.forEachKeyValue(this);

        nar.emotion.stress(linkOverflow);

        for (int i = 0, conceptActivationSize = conceptActivation.size(); i < conceptActivationSize; i++) {
            ObjectFloatPair<Concept> a = conceptActivation.get(i);
            nar.activate(a.getOne(), a.getTwo());
        }

    }

    @Override
    public void value(Term k, float v) {
        //System.out.println("\t" + k + " " + v);

        Termed kk = nar.concept(k, true);
        if (kk != null) {
            Concept ckk = (Concept) kk;

            tasklink(ckk, v);

            float conceptActivationRate = 1f;
            conceptActivation.add(new LightObjectFloatPair(ckk, v * conceptActivationRate));

        } else {
            kk = k;
        }

        termBidi(kk, v, v);
    }

    @Nullable
    void link(@NotNull Term targetTerm, float scale, int depth) {

        float thisScale = scale;

        int nextDepth = depth + 1;
        if (targetTerm instanceof Compound && (nextDepth <= termlinkDepth)) {

            int n = targetTerm.size();
            if (n > 0) {
                float childScale = ((1f - parentRetention) * scale) / (n);
                if (childScale >= minScale) {
                    Compound targetCompound = (Compound) targetTerm;
                    Term[] children = targetCompound.terms();
                    for (Term t : children)
                        link(t.unneg(), childScale, nextDepth); //link and recurse to the concept

                    thisScale = scale * parentRetention;
                }
            }
        }

        spread.addToValue(targetTerm, thisScale);

    }

    final void termBidi(@NotNull Termed tgt, float tlForward, float tlReverse) {

        //System.out.println( "\t" + origin + " " + src + " " + tgt + " "+ n2(scale));

        Term tgtTerm = tgt.term();

        if (tlForward > 0)
            termlink(origin, tgtTerm, tlForward);

        if (tlReverse > 0 && (tgt instanceof Concept)) {
            Term originTerm = origin.term();
            if (!originTerm.equals(tgtTerm)) {
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