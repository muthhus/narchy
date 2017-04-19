package nars.attention;

import jcog.bag.Bag;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import nars.NAR;
import nars.Task;
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

import static jcog.math.Interval.unionLength;
import static nars.Op.NEG;
import static nars.Op.VAR_QUERY;
import static nars.time.Tense.ETERNAL;

/**
 * activation from a point source to its subterm components (termlink templates)
 */
public class SpreadingActivation extends Activation<Task> implements ObjectFloatProcedure<Termed> {

    private final int termlinkDepth;


    final ObjectFloatHashMap<Termed> spread;

    final float parentRetention;

    transient final int dur; //cache
    transient final float inPri; //cached priority value of input at input
    transient final Term originTerm;

    private static final float temporalSpecificity = 0.25f;

    /**
     * 0.5 = forward/backward termlinking is balanced.
     * > 0.5 = more towards the forward link (ie, origin -> subterms)
     * < 0.5 = more towards the backward link (ie, subterms -> origin)
     */
    private static final float TERMLINK_BALANCE = 0.5f;

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
    public SpreadingActivation(@NotNull Task in, float scale, @NotNull Concept src, int termlinkDepth, ObjectFloatHashMap<Termed> spread, @NotNull NAR nar) {
        super(in, scale, src, nar);

        this.termlinkDepth = termlinkDepth;

        this.spread = spread;

        this.parentRetention = nar.momentum.floatValue();

        this.inPri = in.priSafe(0); // * in.qua(); //activate concept by the priority times the quality
        this.dur = nar.dur();

        Term originTerm = origin.term();
        this.originTerm = originTerm;// instanceof Compound ? nar.pre(originTerm) : originTerm;

        link(src, scale, 0);

        spread.forEachKeyValue(this);

        spread.clear();

        nar.emotion.stress(linkOverflow);

    }


    public static int levels(@NotNull Compound host) {
        switch (host.op()) {
            case PROD:
            case SETe:
            case SETi:
            case IMGe:
            case IMGi:
                return 1;

            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
                return 1;

            case SIM:
                return 2;

            case INH:
                return 3;

            case EQUI:
                //return (host.vars() > 0) ? 3 : 2;
                return 3;

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
    public void value(Termed t, float scale) {
        //System.out.println("\t" + k + " " + v);

        PLink<Termed> lt = nar.activate(t, inPri * scale);
        if (lt != null) {

            Concept u = (Concept) lt.get();
            tasklink(u, scale);

        }

        termBidi(t, scale * TERMLINK_BALANCE, scale * (1f - TERMLINK_BALANCE));
    }

    @Nullable
    void link(@NotNull Termed target, float scale, int depth) {

        boolean isntVariable = !(target instanceof Variable);

        if (isntVariable) {
            Concept termConcept = nar.conceptualize(target);
            if (termConcept != null)
                target = termConcept;
        } else {
            if (target.op()==VAR_QUERY)
                return; //dont create termlinks for query variables
        }

        float parentActivation = scale;
        int nextDepth = depth + 1;
        Term targetTerm = target.term();
        if (nextDepth <= termlinkDepth && targetTerm instanceof Compound) {
            parentActivation = linkSubterms(((Compound) targetTerm).subterms(), scale, nextDepth);
        }

        if (target instanceof AtomConcept) {
            //activation terminating at an atom: activate through Atom links
            parentActivation = activateAtom((AtomConcept) target, scale);
        }

        assert(target.op() != NEG); //should have been un-negated already

        spread.addToValue(target, parentActivation);
    }

    protected float activateAtom(AtomConcept atom, float scale) {

        Bag<Task, PLink<Task>> tlinks = atom.tasklinks();
        int n = tlinks.size();
        if (n > 0) {

            //initially start with a fair budget assuming each link receives a full share
            float subActivation = ((1f - parentRetention) * scale) / (n);
            //final float[] change = {0};
            float parentActivation = (parentRetention * scale);

            long inStart = in.start();
            long inEnd = in.end();

            if (subActivation > minScale) {
                tlinks.commit((b) -> {
                    float subSubActivation = subActivation;/// * b.qua();

                    if (inStart != ETERNAL) {
                        Task bt = b.get();
                        long bs = bt.start();
                        if (bs != ETERNAL) {
                            //Temporal vs. temporal: reduce subActivation by temporal distance

                            long be = bt.end();
                            long timeDistance = Math.max(0,
                                    unionLength(inStart, inEnd, bs, be)
                                            - (inEnd - inStart) //task range
                                            - (be - bs)  //belief range
                                            - dur
                            ); //perceptual duration

                            //multiply by temporal relevancy
                            subSubActivation = subSubActivation * (
                                    (1f - temporalSpecificity) + //min reduction
                                            (temporalSpecificity) * TruthPolation.evidenceDecay(
                                                    1f,
                                                    (int) Math.ceil(dur),
                                                    timeDistance));

                        }
                    }

                    if (subSubActivation >= minScale) {
                        subSubActivation -= b.priAddOverflow(subSubActivation, tlinks); //activate the link
                    }

                    //change[0] += (subActivation - subSubActivation);
                });

                //recoup losses to the parent
                //parentActivation += change[0];
                return parentActivation;

            }

        } else {
            //termlinks?
        }

        return scale;
    }

    protected float linkSubterms(@NotNull TermContainer targetSubs, float scale, int nextDepth) {

        int n = targetSubs.size();
        if (n > 0) {
            float childScale = ((1f - parentRetention) * scale) / (n);
            if (childScale >= minScale) {
                float parentActivation = scale * parentRetention;
                for (int i = 0; i < n; i++) {
                    link(targetSubs.term(i).unneg(), childScale, nextDepth); //link and recurse to the concept
                }
                return parentActivation;
            }
        }

        return scale;
    }

    final void termBidi(@NotNull Termed rcpt, float tlForward, float tlReverse) {

        if (rcpt == this.origin) {
            return;
        }

        Term rcptTerm = rcpt.term();

        if (tlForward > 0)
            termlink(origin, rcptTerm, tlForward);

        if (rcpt instanceof Concept && tlReverse > 0)
            termlink((Concept) rcpt, originTerm, tlReverse);

    }

    void tasklink(Concept rcpt, float scale) {
        rcpt.tasklinks().put(
                new RawPLink(in, inPri),
                //new DependentBLink(src),
                scale, null);
    }

    void termlink(Concept recipient, Term target, float scale) {
        recipient.termlinks().put(new RawPLink(target, inPri), scale, linkOverflow);
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