package nars.attention;

import jcog.bag.Bag;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.BLink;
import nars.budget.Budgeted;
import nars.budget.RawBLink;
import nars.concept.Concept;
import nars.task.TruthPolation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;

/**
 * activation from a point source to its subterm components (termlink templates)
 */
public class SpreadingActivation extends Activation implements ObjectFloatProcedure<Termed> {

    private final int termlinkDepth;

    final float inPri; //cached priority value of input at input

    final ObjectFloatHashMap<Termed> spread;

    final static float parentRetention = 0.5f;

    private final int dur; //cache


    /**
     * runs the task activation procedure
     */
    public SpreadingActivation(@NotNull Task in, @NotNull Concept c, @NotNull NAR nar, float scale, ObjectFloatHashMap<Termed> spread) {
        this(in, scale, c, levels(in.term()),
                spread,
                nar);
    }


    /**
     * unidirectional task activation procedure
     */
    public SpreadingActivation(@NotNull Budgeted in, float scale, @NotNull Concept src, int termlinkDepth, ObjectFloatHashMap<Termed> spread, @NotNull NAR nar) {
        super(in, scale, src, nar);

        this.inPri = in.priSafe(0); // * in.qua(); //activate concept by the priority times the quality

        this.termlinkDepth = termlinkDepth;

        this.spread = spread;

        this.dur = nar.dur();

        link(src.term(), scale, 0);

        spread.forEachKeyValue(this);

        spread.clear();

        nar.emotion.stress(linkOverflow);

    }

    private Consumer<BLink<Task>> newTaskLinkUpdate(Task in) {
        return null;
    }

    public static int levels(@NotNull Compound host) {
        switch (host.op()) {
            case SETe:
            case SETi:
            case IMGe:
            case IMGi:
            case PROD:
            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
                return 2;


            case SIM:
            case INH:
                return 2;

            case EQUI:
                //return (host.vars() > 0) ? 3 : 2;
                return 2;

            case IMPL:
            case CONJ:
                return 3;
//                int s = host.size();
//                if (s <= Param.MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES) {
//                    int vars = host.vars();
//                    return (vars > 0) ? 3 : 2;
//                } else {
//                    return 2;
//                    //return (vars > 0) ? 2 : 1; //prevent long conjunctions from creating excessive templates
//                }

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


        float parentScale = scale;

        boolean isntVariable = !(targetTerm instanceof Variable);


        Termed linkedTerm;
        if (isntVariable) {
            Concept termConcept = nar.concept(targetTerm, false);
            if (termConcept != null)
                linkedTerm = termConcept;
            else
                linkedTerm = targetTerm;
        } else {
            linkedTerm = targetTerm;
        }


        int nextDepth = depth + 1;
        if (nextDepth <= termlinkDepth) {

            if (targetTerm instanceof Compound) {
                Compound targetCompound = (Compound) targetTerm;
                @NotNull TermContainer targetSubs = targetCompound.subterms();
                int n = targetSubs.size();
                if (n > 0) {
                    float childScale = ((1f - parentRetention) * scale) / (n);
                    if (childScale >= minScale) {
                        parentScale = scale * parentRetention;
                        for (int i = 0; i < n; i++) {
                            link(targetSubs.term(i).unneg(), childScale, nextDepth); //link and recurse to the concept
                        }
                    }
                }
            } else if (linkedTerm instanceof Concept) {
                //activate through Atom's termlinks
                Bag<Task,BLink<Task>> tlinks = ((Concept) linkedTerm).tasklinks();
                int n = tlinks.size();
                if (n > 0) {
                    float maxSubScale = ((1f - parentRetention) * scale) / (n);
                    if (maxSubScale >= minScale) {
                        parentScale = scale * parentRetention;
                        if (in instanceof Task) {

                            long target = ((Task)in).mid();

                            tlinks.commit((b) -> {
                                float p =
                                        //b.priSafe(0)
                                        b.qua()
                                                * maxSubScale ;
                                Task bt = b.get();
                                if (target!=ETERNAL && !bt.isEternal()) {
                                    //decrease by temporal irrelevancy
                                    p *= TruthPolation.evidenceDecay(1, dur, Math.abs(bt.mid() - target));
                                }

                                if (p >= minScale) {
//                                {
//                                    //activate the concept
//                                    spread.addToValue(b.get(), p);
//                                }

                                    {
                                        //activate the link
                                        b.priAdd(p);
                                    }
                                }
                            });
                        }

                    }
                }
            }
        }

        spread.addToValue(linkedTerm, parentScale);
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
        target.tasklinks().put(
                //new DependentBLink(src),
                new RawBLink(src, src.pri(), src.qua()),
                scale, null);
    }

    void termlink(Concept from, Term to, float scale) {
        from.termlinks().put(new RawBLink(to, in), scale, linkOverflow);
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