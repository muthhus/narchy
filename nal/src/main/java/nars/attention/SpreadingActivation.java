package nars.attention;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.link.BLink;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * activation from a point source to its subterm components (termlink templates)
 */
public class SpreadingActivation extends Activation implements ObjectFloatProcedure<Termed> {

    private final int termlinkDepth;

    final float inPri; //cached priority value of input at input

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

        this.inPri = in.priSafe(0); // * in.qua(); //activate concept by the priority times the quality

        this.termlinkDepth = termlinkDepth;

        Term srcTerm = src.term();

        spread = new ObjectFloatHashMap<>(srcTerm.volume()*4 /* estimate */);

        link(srcTerm, scale, 0);

        spread.forEachKeyValue(this);

        nar.emotion.stress(linkOverflow);

    }

    public static int levels(@NotNull Compound host) {
        switch (host.op()) {
            case SETe:
            case SETi:
            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
            case IMGe:
            case IMGi:
            case PROD:
                return 1;

            case INH:
            case SIM:
                return 2;

            case IMPL:
            case EQUI:
                return (host.vars() > 0) ? 3 : 2;
            case CONJ:

                int s = host.size();
                if (s <= Param.MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES) {
                    int vars = host.vars();
                    return (vars > 0) ? 3 : 2;
                } else {
                    return 2;
                    //return (vars > 0) ? 2 : 1; //prevent long conjunctions from creating excessive templates
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

            float qv = v * inPri;
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

        boolean isntVariable = !(targetTerm instanceof Variable);


        Termed linkedTerm;
        if (isntVariable) {
            Concept termConcept = nar.concept(targetTerm, true);
            if (termConcept != null)
                linkedTerm = termConcept;
            else
                return;
        } else {
            linkedTerm = targetTerm;
        }


        int nextDepth = depth + 1;
        if (nextDepth <= termlinkDepth) {

            if (targetTerm instanceof Compound) {
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
            } else if (linkedTerm instanceof Concept) {
                //activate (but not link to) Atom's termlinks
                Bag<Term> tlinks = ((Concept) linkedTerm).termlinks();
                int n = tlinks.size();
                if (n > 0) {
                    float maxSubScale = ((1f - parentRetention) * scale) / (n);
                    if (maxSubScale >= minScale) {
                        for (BLink<Term> b : tlinks) {
                            if (b!=null) {
                                Term key = b.get();
                                float p = b.priSafe(0) * maxSubScale;
                                if (p >= minScale)
                                    spread.addToValue(key, p);
                            }
                        }
                    }
                }
            }
        }

        spread.addToValue(linkedTerm, thisScale);

    }

    final void termBidi(@NotNull Termed tgt, float tlForward, float tlReverse) {

        //System.out.println( "\t" + origin + " " + src + " " + tgt + " "+ n2(scale));

        Term tgtTerm = tgt.term();

        if (tlForward > 0)
            termlink(origin, tgtTerm, tlForward);

        if (tgt!=origin /*(fast test to eliminate reverse self link)*/ && tlReverse > 0 && (tgt instanceof Concept)) {
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