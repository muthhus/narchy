package nars.attention;

import jcog.bag.Bag;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.BLink;
import nars.budget.Budgeted;
import nars.budget.RawBLink;
import nars.concept.AtomConcept;
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


    final ObjectFloatHashMap<Termed> spread;

    final static float parentRetention = 0.5f;

    transient final int dur; //cache
    transient final float inPri; //cached priority value of input at input
    transient final float inQua; //cached quality value of input at input

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

        this.termlinkDepth = termlinkDepth;

        this.spread = spread;

        this.inPri = in.priSafe(0); // * in.qua(); //activate concept by the priority times the quality
        this.inQua = in.qua();
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
                return 1;


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


        float parentActivation = scale;

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
                        parentActivation = scale * parentRetention;
                        for (int i = 0; i < n; i++) {
                            link(targetSubs.term(i).unneg(), childScale, nextDepth); //link and recurse to the concept
                        }
                    }
                }
            }
        }

        if (linkedTerm instanceof AtomConcept) {
            //activation terminating at an atom: activate through Atom links
            if (in instanceof Task) {
                Bag<Task, BLink<Task>> tlinks = ((Concept) linkedTerm).tasklinks();
                int n = tlinks.size();
                if (n > 0) {

                    //initially start with a fair budget assuming each link receives a full share
                    float subActivation = ((1f - parentRetention) * scale) / (n);
                    final float[] change = {0};
                    parentActivation = subActivation * n;

                    long inStart = ((Task) in).start();
                    long inEnd = ((Task) in).end();


                    tlinks.commit((b) -> {
                        float subSubActivation =
                                //b.priSafe(0)
                                b.qua() * subActivation;

                        if (subSubActivation >= minScale) {
                            Task bt = b.get();
                            if (inStart != ETERNAL) {
                                long timeDistance = bt.timeDistance(inStart);
                                if (timeDistance != ETERNAL) {
                                    timeDistance = Math.min(timeDistance, bt.timeDistance(inEnd)); //HACK do this faster

                                    //increase duration up to 2x in proportion to the conf of a belief or goal
                                    int eDur =
                                            (int) Math.ceil(dur * (1f + (bt.isBeliefOrGoal() ? bt.conf() : 0f)));

                                    //multiply by temporal relevancy
                                    subSubActivation *= TruthPolation.evidenceDecay(
                                            1 + (change[0] / 2f / subActivation), //boost with up to half of collected change
                                            eDur,
                                            timeDistance);
                                }
                            }

                            if (subSubActivation >= minScale) {
                                change[0] += b.priAddOverflow(subSubActivation); //activate the link
                            } else {
                                subSubActivation = 0;
                            }

                        } else {
                            subSubActivation = 0;
                        }

                        change[0] += (subActivation - subSubActivation);
                    });

                    //recoup losses to the parent
                    parentActivation += change[0];
                }
            } else {
                //termlinks?
            }
        }

        spread.addToValue(linkedTerm, parentActivation);
    }

    final void termBidi(@NotNull Termed tgt, float tlForward, float tlReverse) {

        //System.out.println( "\t" + origin + " " + src + " " + tgt + " "+ n2(scale));

        Term tgtTerm = tgt.term();

        if (tlForward > 0)
            termlink(origin, tgtTerm, tlForward);

        if (tgt != origin /*(fast test to eliminate reverse self link)*/ && tlReverse > 0 && (tgt instanceof Concept)) {
            Term originTerm = origin.term();
            if (!originTerm.equals(tgtTerm)) {
                termlink((Concept) tgt, originTerm, tlReverse);
            }
        }

    }

    void tasklink(Concept target, float scale) {
        target.tasklinks().put(
                new RawBLink(in, inPri, inQua),
                    //new DependentBLink(src),
                scale, null);
    }

    void termlink(Concept from, Term to, float scale) {
        from.termlinks().put(new RawBLink(to, inPri, inQua), scale, linkOverflow);
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