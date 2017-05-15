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
import nars.term.atom.Atom;
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


    static final ThreadLocal<ObjectFloatHashMap<Termed>> activationMapThreadLocal =
            ThreadLocal.withInitial(ObjectFloatHashMap::new);



    final ObjectFloatHashMap<Termed> spread;


    /**
     * cached for fast access
     */
    transient final int dur;

    /**
     * priority value of input at input, cached for fast access
     */
    transient final float inPri;

    /**
     * cached
     */
    transient final Term originTerm;


    /**
     * momentum > 0.5 means parents preserve more of the priority than sharing with children
     */
    final float momentum;

    /**
     * values closer to zero mean atom tasklink activation is less filtered by time;
     * values closer to one mean atom tasklink activation is more filtered to specific time
     */
    private static final float temporalSpecificity = 0.5f;

    /**
     * 0.5 = forward/backward termlinking is balanced.
     * > 0.5 = more towards the forward link (ie, origin -> subterms)
     * < 0.5 = more towards the backward link (ie, subterms -> origin)
     */
    private static final float TERMLINK_BALANCE = 0.5f;

    private final float linkScale;

    /**
     * runs the task activation procedure
     */
    public SpreadingActivation(@NotNull Task in, @NotNull Concept c, @NotNull NAR nar, float scale) {
        this(in, scale, c,  activationMapThreadLocal.get(), nar);
    }


    /**
     * unidirectional task activation procedure
     */
    public SpreadingActivation(@NotNull Task in, float scale, @NotNull Concept src, ObjectFloatHashMap<Termed> spread, @NotNull NAR nar) {
        super(in, scale, src, nar);

        this.momentum = nar.momentum.floatValue();
        this.linkScale = nar.linkActivation.floatValue();

        this.inPri = in.priSafe(0); // * in.qua(); //activate concept by the priority times the quality
        this.dur = nar.dur();

        Term originTerm = origin.term();
        this.originTerm = originTerm;// instanceof Compound ? nar.pre(originTerm) : originTerm;

        this.spread = spread;
        spread.clear();

        link(src, scale, 0);

        spread.forEachKeyValue(this);

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
                return 2;

            case EQUI:
                //return (host.vars() > 0) ? 3 : 2;
                return 2;

            case IMPL:
                return 3;
            case CONJ:
                return 2;

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

    public static Activation activate(Task t, @NotNull NAR n, @NotNull Concept c, float scale) {
        //return new DepthFirstActivation(input, this, nar, nar.priorityFactor.floatValue());

        //float s = scale * (0.5f + 0.5f * pri(c, 1));
        return new SpreadingActivation(t, c, n, scale);
    }

    @Override
    public void value(Termed c, float scale) {
        //System.out.println("\t" + k + " " + v);

        scale *= linkScale;

        termBidi(c, scale * TERMLINK_BALANCE, scale * (1f - TERMLINK_BALANCE));

        if (c instanceof Concept) {
            tasklink((Concept) c, scale);
        } else if (c instanceof AtomConcept) {
            activateAtom((AtomConcept) c, scale);
        }

    }

    @Nullable
    void link(@NotNull Termed target, float scale, int depth) {

        if ((target instanceof Variable)) {
            if (target.op() == VAR_QUERY)
                return; //dont create termlinks to query variable subterms
        } else {
            @Nullable PLink<Concept> termConcept = nar.activate(target, inPri * scale);
            if (termConcept != null)
                target = termConcept.get();
        }

        final float parentActivation;

        /*if (depth + 1 <= termlinkDepth)*/ {

            if (target instanceof Compound) {
                //recurse
                parentActivation = linkSubterms(((Compound) target).subterms(), scale, depth + 1);
            } else if (target instanceof AtomConcept) {
                //activation terminating at an atom: activate through Atom links
                parentActivation = scale;
            } else {
                parentActivation = 0;
            }
        } /*else {
            parentActivation = scale;
        }*/

        assert (target.op() != NEG); //should have been un-negated already

        if (parentActivation > 0)
            spread.addToValue(target, parentActivation);

    }

    protected float activateAtom(AtomConcept atom, float scale) {

        Bag<Task, PLink<Task>> tlinks = atom.tasklinks();
        int n = tlinks.size();
        if (n > 0) {

            //initially start with a fair budget assuming each link receives a full share
            float subActivation = ((1f - momentum) * scale) / (n);
            //final float[] change = {0};

            long inStart = in.start();
            long inEnd = in.end();

            float[] additionalPressure = {0};
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

                if (subSubActivation >= PLink.EPSILON) {
                    subSubActivation -= b.priAddOverflow(subSubActivation, additionalPressure); //activate the link
                }

                //change[0] += (subActivation - subSubActivation);
            });
            if (additionalPressure[0] > 0) {
                tlinks.pressurize(additionalPressure[0]);
            }

            //recoup losses to the parent
            //parentActivation += change[0];
            float parentActivation = (momentum * scale);
            return parentActivation;


        } else {
            //termlinks?
        }

        return scale;
    }

    protected float linkSubterms(@NotNull TermContainer targetSubs, float scale, int nextDepth) {

        int n = targetSubs.size();
        if (n > 0) {
            float childScale = ((1f - momentum) * scale) / (n);

            for (int i = 0; i < n; i++) {
                link(targetSubs.sub(i).unneg(), childScale, nextDepth); //link and recurse to the concept
            }
            float parentActivation = scale * momentum;
            return parentActivation;

        }

        return scale;
    }


    final void termBidi(@NotNull Termed rcpt, float tlForward, float tlReverse) {

        if (rcpt == this.origin)
            return;

        Term rcptTerm = rcpt.term();

        if (tlForward > 0)
            termlink(origin, rcptTerm, tlForward);

        if (rcpt instanceof Concept && tlReverse > 0)
            termlink((Concept) rcpt, originTerm, tlReverse);

    }

    void tasklink(Concept rcpt, float scale) {
        float p = inPri * scale;
        if (p >= PLink.EPSILON) {
            rcpt.tasklinks().put(
                    new RawPLink(in, p),
                    //new DependentBLink(src),
                    1f, null);
        }
    }

    void termlink(Concept recipient, Term target, float scale) {
        float p = inPri * scale;
        if (p >= PLink.EPSILON)
            recipient.termlinks().put(new RawPLink(target, p), 1f, linkOverflow);
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