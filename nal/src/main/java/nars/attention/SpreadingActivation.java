package nars.attention;

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
public class SpreadingActivation extends Activation implements ObjectFloatProcedure<Termed> {

    private final int termlinkDepth;

    @NotNull public final Budgeted in;

    final ObjectFloatHashMap<Termed> spread;

    final static float parentRetention = 0.5f;

    /**
     * runs the task activation procedure
     */
    public SpreadingActivation(@NotNull Budgeted in, @NotNull Concept c, @NotNull NAR nar, float scale) {
        this(in, scale, c, in instanceof Task ? levels(((Task)in).term()) : Param.ACTIVATION_TERMLINK_DEPTH, nar);
    }

    /**
     * unidirectional task activation procedure
     */
    public SpreadingActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, int termlinkDepth, @NotNull NAR nar) {
        super(in, scale, src, nar);
        this.in = in;
        this.termlinkDepth = termlinkDepth;

        Term srcTerm = src.term();

        spread = new ObjectFloatHashMap<>(srcTerm.volume());

        link(srcTerm, scale, 0);

        spread.forEachKeyValue(this);

        nar.emotion.stress(linkOverflow);

    }

    public static int levels(@NotNull Compound host) {
        switch (host.op()) {
            case PROD:
            case SETe:
            case SETi:
            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
            case IMGe:
            case IMGi:
                return 1;

            case INH:
                return 2;
            case SIM:
                return 2;

            case IMPL:
            case EQUI:
                return (host.vars() > 0) ? 3 : 2;
            case CONJ: {

                int s = host.size();
                int vars = host.vars();
                if (s <= Param.MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES) {
                    return (vars > 0) ? 3 : 2;
                } else {
                    return 2;
                    //return (vars > 0) ? 2 : 1; //prevent long conjunctions from creating excessive templates
                }
            }

            default:
                throw new UnsupportedOperationException("unhandled operator type: " + host.op());
        }
    }

    @Override
    public void value(Termed k, float v) {
        //System.out.println("\t" + k + " " + v);


        Termed kk = nar.concept(k, true);
        if (kk != null) {
            Concept ckk = (Concept) kk;

            tasklink(ckk, v);

            float qv = v * in.pri() * in.qua(); //activate concept by the priority times the quality
            if (qv >= Param.BUDGET_EPSILON)
                nar.activate(ckk, qv);

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

        Concept termConcept = nar.concept(targetTerm, true);
        if (termConcept!=null)
            spread.addToValue(termConcept, thisScale);

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