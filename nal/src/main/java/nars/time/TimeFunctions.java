package nars.time;

import jcog.Util;
import jcog.math.Interval;
import nars.$;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.derive.meta.Conclude;
import nars.derive.meta.OccurrenceSolver;
import nars.derive.rule.PremiseRule;
import nars.premise.Derivation;
import nars.premise.Premise;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.$.terms;
import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.index.TermBuilder.isTrueOrFalse;
import static nars.task.Revision.chooseByConf;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.*;
import static nars.time.TimeFunctions.occInterpolate;

/**
 * Strategies for solving temporal components of a derivation
 */
@FunctionalInterface
public interface TimeFunctions {

    /**
     * @param derived   raw resulting untemporalized derived term that may or may not need temporalized and/or occurrence shifted as part of a derived task
     * @param p         current match context
     * @param d         derivation rule being evaluated
     * @param occReturn holds the occurrence time as a return value for the callee to use in building the task
     * @param confScale
     * @return
     */
    @Nullable Compound compute(@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, @NotNull float[] confScale);


    static long earlyOrLate(long t, long b, boolean early) {
        boolean tEternal = t == ETERNAL;
        boolean bEternal = b == ETERNAL;
        if (!tEternal && !bEternal) {
            if (early)
                return t <= b ? t : b;
            else
                return t >= b ? t : b;
        }
        if (!bEternal/* && tEternal*/) {
            return b;
        }
        if (!tEternal/* && bEternal*/) {
            return t;
        }
        return ETERNAL;
    }

    //nars.Premise.OccurrenceSolver latestOccurrence = (t, b) -> earlyOrLate(t, b, false);
    OccurrenceSolver earliestOccurrence = (t, b) -> earlyOrLate(t, b, true);
    OccurrenceSolver latestOccurence = (t, b) -> earlyOrLate(t, b, false);

    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtTminB = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, +1);
    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtBminT = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, -1);
    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtIntersect = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, 0);

    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtUnion = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> {
        //TODO
        return dtDiff(derived, p, occReturn, 2);
    };
    TimeFunctions dtUnionReverse = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> {
        //TODO
        return dtDiff(derived, p, occReturn, -2);
    };

    /**
     * does nothing but set DTternal; the input tasks will be considered to be eternal
     */
    @Nullable TimeFunctions dternal = (derived, p, d, occReturn, confScale) -> dt(derived, DTERNAL, occReturn);

    @NotNull
    static Compound dtDiff(@NotNull Compound derived, @NotNull Derivation p, @NotNull long[] occReturn, int polarity) {

        Compound taskTerm = (Compound) p.taskTerm.unneg();
        Termed<Compound> beliefTerm = p.beliefTerm;

        int dt;
        int ttd = taskTerm.dt();
        int btd = beliefTerm.term().dt();
        if (ttd != DTERNAL && btd != DTERNAL) {
            switch (polarity) {

                case 2:  //union
                    dt = -(ttd + btd);
                    break;
                case -2:  //unionReverse
                    dt = (ttd + btd);
                    break;
                case -1:
                case +1: //difference: -1 or +1
                    dt = (ttd - btd);
                    break;

                case 0:
//                    dt = (ttd + btd) / 2; //intersect: 0
//                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        } else if (ttd != DTERNAL) {
            dt = ttd;
        } else if (btd != DTERNAL) {
            dt = btd;
        } else {
            dt = DTERNAL;
        }

        if ((polarity == 0) || (polarity == 2) || (polarity == -2)) {
            occReturn[0] = occurrenceTarget(p.premise, earliestOccurrence); //TODO CALCULATE

            //restore forward polarity for function call at the end
            polarity = 1;
        } else {
            //diff
            occReturn[0] = occurrenceTarget(p.premise, earliestOccurrence);
        }

        return deriveDT(derived, polarity, dt, occReturn);
    }

    /**
     * simple delta-time between task and belief resulting in the dt of the temporal compound.
     * this assumes the conclusion term is size=2 compound.
     * no occurence shift
     * should be used in combination with a "premise Event" precondition
     */
    TimeFunctions occForward = (derived, p, d, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, +1);
    TimeFunctions occReverse = (derived, p, d, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, -1);


    @NotNull
    static Compound occBeliefMinTask(@NotNull Compound derived, @NotNull Derivation p, @NotNull long[] occReturn, int polarity) {

        long beliefStart = p.belief.start();
        long taskStart = p.task.start();

        if (beliefStart != ETERNAL && taskStart != ETERNAL) {

            int dt = (int) (beliefStart - taskStart); //TODO check valid int/long conversion
            long start = occurrenceTarget(p.premise, earliestOccurrence);

            occReturn[0] = start;

            //HACK to handle commutive switching so that the dt is relative to the effective subject
            if (dt != 0 && dt != DTERNAL && derived.op().commutative) {

                Term bt = p.beliefTerm;
                Term d0 = derived.term(0);

                if (derivationMatch(bt, d0))
                    dt *= -1;
            }


            return deriveDT(derived, polarity, dt, occReturn);


        }
        if (beliefStart != ETERNAL) {
            occReturn[0] = beliefStart;
            occReturn[1] = p.belief.end();
            //eventDelta = DTERNAL;
        } else if (taskStart != ETERNAL) {
            occReturn[0] = taskStart;
            occReturn[1] = p.task.end();
            //eventDelta = DTERNAL;
        } else {
            //eventDelta = DTERNAL;
        }

        return derived;
    }

    @Deprecated
    static boolean derivationMatch(@NotNull Term a, @NotNull Term b) {
        return /*productNormalize*/(a.unneg()).equalsIgnoringVariables(/*productNormalize*/b, true);
    }

//    static boolean derivationMatch(@NotNull Term a, @NotNull Term b, @NotNull Derivation p) {
//        Term pa = resolve(p, a);
//        if (pa!=null) {
//            Term pb = resolve(p, b);
//            if (pb!=null) {
//                return pa.unneg().equalsIgnoringVariables(pb);
//            }
//        }
//        return false;
//    }


    /**
     * copiesthe 'dt' and the occurence of the task term directly
     */
    TimeFunctions dtTaskExact = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtExact(derived, occReturn, p, true);
    TimeFunctions dtBeliefExact = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtExact(derived, occReturn, p, false);


    /**
     * special handling for dealing with detaching, esp. conjunctions which involve a potential mix of eternal and non-eternal premise components
     */
    @Nullable TimeFunctions decomposeTask = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) ->
            decompose(derived, p, occReturn, true);
    @Nullable TimeFunctions decomposeBelief = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) ->
            decompose(derived, p, occReturn, false);

    @Nullable TimeFunctions decomposeBeliefLate = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) ->
            decomposeLate(derived, p, occReturn, false);

    static Compound decomposeLate(@NotNull Compound derived, @NotNull Derivation p, @NotNull long[] occReturn, boolean b) {
        Compound x = decompose(derived, p, occReturn, b);
        if (p.task.isGoal() && (x!=null) && (occReturn[0]!=ETERNAL)) {
            long taskStart = p.task.start();
            if (taskStart!=ETERNAL) {
                //dont derive a past-tense goal (before the task)
                if (taskStart > occReturn[0]) {
                    if (occReturn[1] == ETERNAL) occReturn[1] = occReturn[0]; //HACK
                    long range = occReturn[1] - occReturn[0];

                    occReturn[0] = taskStart;
                    occReturn[1] = taskStart + range;
                }
            }
        }
        return x;
    }


    /** the 2-ary result will have its 'dt' assigned by the occurrence of its subterms in the task's compound */
    @Nullable TimeFunctions decomposeTaskComponents = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> {

        if (derived.size()==2) {

            Compound from = p.task.term();
            int occA = from.subtermTime(derived.term(0));

            if (occA != DTERNAL) {

                int occB = from.subtermTime(derived.term(1));

                if (occB != DTERNAL) {
                    if (!p.task.isEternal()) {
                        occReturn[0] = p.task.start() + Math.min(occA, occB);
                    }
                    int dt = occB - occA;
                    return deriveDT(derived, +1, dt, occReturn);
                }
            }
        }

        return null;
    };

    /*** special case for decomposing conjunctions in the task slot */
    @Nullable TimeFunctions decomposeTaskSubset = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> {
        Task task = p.task;
        Compound taskTerm = task.term();

        int taskSize = taskTerm.size();

        if (derived.op() == CONJ && (task.volume() == derived.volume() && taskSize == derived.size() && task.term().vars() == derived.vars())) {
            //something wrong happened with the ellipsis selection.
            //being a decomposition it should produce a smaller result
            throw new InvalidTermException(derived.op(), derived.terms(), "ellipsis commutive match fault: same as parent");
        }

        if (taskSize <= 2) { //conjunction of 1 can not occur actually, but for completeness use the <=
            //a decomposition of this will result in a complete subterm (half) of the input.
            //if (dt!=DTERNAL || dt!=XTERNAL || dt!=0) {
                //call "decomposeTask"
            //return decompose(derived, p, occReturn, true);


            Term resolvedTaskTerm = resolve(p, taskTerm);
            int derivedInTask = resolvedTaskTerm.subtermTime(derived);
            if (derivedInTask!=DTERNAL) {
                if (!task.isEternal()) {
                    occReturn[0] = task.start() + derivedInTask;
                    occReturn[1] = occReturn[0] + (derived.op()==CONJ ? derived.dtRange() : 0);
                    return derived;
                } else if (p.belief != null && !p.belief.isEternal()) {
                    int timeOfBeliefInTask = resolvedTaskTerm.subtermTime(resolve(p,p.beliefTerm));
                    if (timeOfBeliefInTask==DTERNAL)
                        timeOfBeliefInTask = 0;
                    long taskOcc = p.belief.start() - timeOfBeliefInTask;
                    occReturn[0] = taskOcc + derivedInTask;
                    occReturn[1] = occReturn[0] + (derived.op()==CONJ ? derived.dtRange() : 0);
                    return derived;
                } else {
                    //both eternal - no temporal basis
                    return null;
                }
            }
        } else {

            //3 or more
            if (derived.op() == CONJ) {
                int dt = task.dt(); //copy either the DTERNAL or 0 commutive timing

                if (!task.isEternal()) {
                    occReturn[0] = task.start();
                } else if ((p.belief != null && !p.belief.isEternal())) {
                    Term resolvedTaskTerm = resolve(p, taskTerm);
                    int timeOfBeliefInTask = resolvedTaskTerm.subtermTime(p.beliefTerm);
                    occReturn[0] = p.belief.start() - timeOfBeliefInTask;
                } else {
                    return null;
                }

                occReturn[1] = occReturn[0] + derived.dtRange();

                return deriveDT(derived, +0, dt, occReturn);
            }
        }

        //throw new UnsupportedOperationException();
        //return derived;
        return null;
    };

    @Nullable
    static Compound decompose(@NotNull Compound derived, @NotNull Derivation p, @NotNull long[] occReturn, boolean decomposeTask) {


        Task task = p.task;
        Task belief = p.belief;

        Task otherTask = (decomposeTask) ? belief : task;

        Compound decomposedTerm = (Compound) (decomposeTask ? p.taskTerm : p.beliefTerm);

        //occOther: the non-decomposed counterpart of the premise

//        long occDecomposed = task.start();
//        long occOther = belief!=null ? belief.start() : ETERNAL;
//        if (task.start() == ETERNAL && belief!=null) {
//            occDecomposed = belief.start();
//            occOther = task.start();
//        }

        long occDecomposed = decomposeTask ? task.start() : (belief != null ? belief.start() : task.start());
        long occOther = (otherTask != null) ? otherTask.start() : ETERNAL;


        if ((occDecomposed == ETERNAL) && (occOther == ETERNAL)) {
            //no temporal basis that can apply. only derive an eternal result
            // if there is no actual temporal relation in the decomposition

            int ddt = decomposedTerm.dt();
            if ((ddt == 0 || ddt == DTERNAL || ddt == XTERNAL)) {
                return derived; //allow eternal decompose since no temporal information is inside the decomposed term
            } else {
                return noTemporalBasis(derived);
            }

        }


        long occ = ETERNAL;


        if (decomposedTerm.size() != 2) {
            //probably a (&&+0, ...)
            occ = occDecomposed != ETERNAL ? occDecomposed : occOther;
        } else {

            @Nullable Term rDecomposed = resolve(p, decomposedTerm);

            if (rDecomposed!=null) {

                @Nullable Term rDerived = resolve(p, derived);

                if (rDerived != null) {
                    int dt = rDecomposed.subtermTime(rDerived);

                    //prefer the task's occurrence first
                    if (dt != DTERNAL) {

                        if (!task.isEternal()) {

                            occ = task.start();

                            Term rTaskTerm = resolve(p, task.term());
                            if (rTaskTerm!=null) {
                                Term rOtherTerm = resolve(p, p.beliefTerm.term());
                                if (rOtherTerm != null) {
                                    int taskInDecomposed = rDecomposed.subtermTime(rTaskTerm);
                                    if (taskInDecomposed!= DTERNAL) { //???
                                        int otherInDecomposed = rDecomposed.subtermTime(rOtherTerm);
                                        if (otherInDecomposed != DTERNAL) { //???
                                            occ = task.start() - (otherInDecomposed + rDerived.dtRange()) - taskInDecomposed + dt;
                                        }
                                    }
                                }
                            }

                        } else if (occDecomposed != ETERNAL) {

                            occ = occDecomposed + dt;

                        }

                    }
                }

                if (occ == ETERNAL && occOther != ETERNAL) {


                    @Nullable Term rOtherTerm = resolve(p, decomposeTask ? p.beliefTerm.term() : p.taskTerm);
                    if (rOtherTerm != null) {

                        //                        if (derivationMatch(rOtherTerm, derived, p)) {
                        //                            occ = occOther; } else
                        int otherInDecomposed = rDecomposed.subtermTime(rOtherTerm);
                        if (decomposedTerm.dt() == 0 && otherInDecomposed == 0) {
                            //special case for &&+0 having undergone some unrecognizable change
                            occ = occOther - 0; //+0 should ensure it has the same time as this sibling event

                        } else if (rDerived != null) { //{ && otherInDecomposed != DTERNAL) {
                            int derivedInDecomposed = rDecomposed.subtermTime(rDerived);
                            if (derivedInDecomposed != DTERNAL) {
                                occ = occOther + derivedInDecomposed;
                                if (otherInDecomposed != DTERNAL) { //???
                                    if (derivedInDecomposed == 0) {
                                        occ -= otherInDecomposed + rDerived.dtRange();
                                    } else {
                                        occ -= otherInDecomposed; //<---- TODO subsent_1
                                    }
                                }

                            } else {
                                return null; //could not back substitute resolve
                            }
                        }

                    }

                }

                //default:
                if (occ == ETERNAL)
                    occ = occOther;

            }



            if (occ == ETERNAL) {
                return noTemporalBasis(derived);
            }

        }


        occReturn[0] = occ;

        //TODO decide if this is right
        int dt = derived.dt();
        if (derived.op()==CONJ && dt !=DTERNAL)
            occReturn[1] = occ + derived.dtRange();

        return derived;

    }

    @Nullable
    static Term resolve(@NotNull Derivation p, @NotNull Term x) {
        Term y;
        try {
            y = p.resolve(p.index.productNormalize(x));
        } catch (InvalidTermException e) {
            //failed, just return the input
            y = null;
        }
        return y!=null ? y : x;
    }

    @Nullable
    static Compound noTemporalBasis(@NotNull Compound derived) {
        if (Param.DEBUG_EXTRA)
            throw new InvalidTermException(derived.op(), derived.dt(), "no basis for relating other occurrence to derived", derived.terms()
            );
        else
            return null;
    }


    @NotNull
    static Compound dtExact(@NotNull Compound derived, @NotNull long[] occReturn, @NotNull Derivation p, boolean taskOrBelief) {

        Term dtTerm = taskOrBelief ? p.taskTerm.unneg() : p.beliefTerm;

        Task t = p.task;
        Task b = p.belief;

        long tOcc = t.start();
        if (b!=null) {
            if (t.isQuestOrQuestion() || tOcc==ETERNAL)
                tOcc = b.start(); //use belief time when task is a question, or task is eternal
        }

        if (!taskOrBelief && b != null) {
            //if (b.occurrence()!=ETERNAL) {
            int derivedInT = dtTerm.subtermTime(derived);
            if (derivedInT == DTERNAL && derived.op() == Op.IMPL) {
                //try to find the subtermTime of the implication's subject
                derivedInT = dtTerm.subtermTime(derived.term(0));
            }

            if (derivedInT == DTERNAL) {
                derivedInT = 0;
            }

            if (tOcc != ETERNAL)
                occReturn[0] = tOcc + derivedInT;

//            } else if (t.occurrence()!=ETERNAL) {
//                //find the offset of the task term within the belief term, and then add the task term's occurrence
//                occReturn[0] =
//            }
        } else {
            occReturn[0] = tOcc; //the original behavior, but may not be right
        }



        int dtdt = ((Compound) dtTerm).dt();
        return deriveDT(derived, +1, dtdt, occReturn);

    }

    @NotNull
    static Compound deriveDT(@NotNull Compound derived, int polarity, int eventDelta, @NotNull long[] occReturn) {

        int dt = eventDelta == DTERNAL ? DTERNAL : eventDelta * polarity;

        return dt(derived, dt, occReturn);
    }


    @NotNull TimeFunctions dtCombine = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtCombiner(derived, p, occReturn, false, false);

    @NotNull TimeFunctions dtCombinePre = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtCombiner(derived, p, occReturn, true, false);

    @NotNull TimeFunctions dtCombinePost = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtCombiner(derived, p, occReturn, false, true);


    /**
     * combine any existant DT's in the premise (assumes both task and belief are present)
     */
    @Nullable static Compound dtCombiner(@NotNull Compound derived, @NotNull Derivation p, long[] occReturn, boolean pre, boolean post) {

        Task task = p.task;
        int taskDT = ((Compound) p.taskTerm.unneg()).dt();

        Term bt = p.beliefTerm;
        int beliefDT = (bt instanceof Compound) ? ((Compound) bt.unneg()).dt() : DTERNAL;
        int eventDelta = DTERNAL;

        if (derived.size()>1) {



            if (taskDT == DTERNAL && beliefDT == DTERNAL) {

                //eventDelta = DTERNAL;

            } else if (taskDT != DTERNAL && beliefDT != DTERNAL) {

                if (derived.size() != 2)
                    throw new InvalidTermException(derived.op(), derived.terms(), "expectd arity=2");

                //assume derived has 2 terms exactly
                Term da = derived.term(0);
                Term db = derived.term(1);

                Compound tt = task.term();
                int ta = tt.subtermTime(da);
                int tb = tt.subtermTime(db);

                Compound btc = (Compound) bt;
                int ba = btc.subtermTime(da);
                int bb = btc.subtermTime(db);

                int dtT, dtB;
                if (ta != DTERNAL && tb != DTERNAL && ba != DTERNAL && bb != DTERNAL) {
                    //compare between subterms both present in the premise
                    dtT = tb - ta;
                    dtB = bb - ba;
                } else {
                    //compare between one set of common subterms, the other is a new introduced term
                    if (ta == DTERNAL && ba == DTERNAL) {
                        dtT = taskDT;
                        dtB = beliefDT;
                    } else if (tb == DTERNAL && bb == DTERNAL) {
                        dtT = taskDT;
                        dtB = beliefDT;
                    } else {
                        return null;
                    }
                }

                Task chosen = chooseByConf(task, p.belief, p);
                if (chosen == task)
                    eventDelta = dtT;
                else
                    eventDelta = dtB;


            } else if (taskDT == DTERNAL) {
                eventDelta = beliefDT;
            } else /*if (beliefDT == DTERNAL)*/ {
                eventDelta = taskDT;
            }
        }

        occReturn[0] = occurrenceTarget(p.premise, earliestOccurrence);

        if (pre && derived.term(0) instanceof Compound) {
            //set subterm 0's DT


            Compound preSub = (Compound) derived.term(0);
            boolean neg = preSub.op()==NEG;
            if (neg) {
                preSub = (Compound) preSub.unneg(); //unwrap
            }

            int preDT;
            if ((taskDT != DTERNAL) && (taskDT!=XTERNAL) && (beliefDT != DTERNAL) && (beliefDT!=XTERNAL)) {
                preDT = (taskDT - beliefDT);
                if (!task.term(0).equals(preSub.term(0)))
                    preDT = -preDT; //reverse the order
            } else {
                preDT = DTERNAL;
            }

            Term newPresub = p.index.the( preSub, preDT );

            if (!(newPresub instanceof Compound))
                return null;

            derived = compoundOrNull((p.index.the(derived,
                    new Term[] { $.negIf(newPresub, neg), derived.term(1) })
            ));
        }
        if (post && derived.term(1) instanceof Compound) {

            Compound postSub = (Compound) derived.term(1);
            boolean neg = postSub.op()==NEG;
            if (neg) {
                postSub = (Compound) postSub.unneg(); //unwrap
            }

            int postDT;
            if ((taskDT != DTERNAL) && (taskDT!=XTERNAL) && (beliefDT != DTERNAL) && (beliefDT!=XTERNAL)) {
                postDT = (taskDT - beliefDT);
                if (task.term().size() > 1 && postSub.size() > 1 && !task.term(1).equals(postSub.term(1)))
                    postDT = -postDT; //reverse the order
            } else {
                postDT = DTERNAL;
            }


            //set subterm 1's DT
            Term newSubterm1 = p.index.the((Compound) derived.term(1), postDT);

            if (isTrueOrFalse(newSubterm1))
                return null;


            derived = compoundOrNull(
                    p.index.the(derived, new Term[] { derived.term(0), $.negIf(newSubterm1,neg) } )
            );

        }

        if (derived == null)
            return null;

        return deriveDT(derived, 1, eventDelta, occReturn);
    }


    /**
     * "automatic" implementation of Temporalize, used by default. slow and wrong about 25..30% of the time sux needs rewritten or replaced
     * apply temporal characteristics to a newly derived term according to the premise's
     */
    @NotNull TimeFunctions Auto = (derived, p, d, occReturn, confScale) -> {


        @NotNull PremiseRule rule = d.rule;
        Term tp = rule.getTask();
        Term bp = rule.getBelief();

        //ConceptProcess premise = p.premise;

        Task task = p.task;
        Task belief = p.belief;

        long occ = occInterpolate(task, belief); //reset


        Compound tt = (Compound) p.taskTerm.unneg();
        Term bb = p.beliefTerm; // belief() != null ? belief().term() : null;

        int td = tt.dt();
        int bd = bb instanceof Compound ? ((Compound) bb).dt() : DTERNAL;

        int t = DTERNAL;

        Term cp = d.conclusionPattern; //TODO this may be a wrapped immediatefunction?

        if (derived.op().temporal && cp instanceof Compound) {

            Compound ccc = (Compound) cp;
            Term ca = ccc.term(0);

            //System.out.println(tt + " "  + bb);

        /* CASES:
            conclusion pattern size 1
                equal to task subterm
                equal to belief subterm
                unique term
            conclusion pattern size 2 (a, b)
                a equal to task subterm
                b equal to task subterm
                a equal to belief subterm
                b equal to belief subterm

         */
            int s = cp.size();
            if (s == 2) {
                Term cb = ccc.term(1);

                //chained relations
//                if (td != DTERNAL && bd != DTERNAL && (tp.size() == 2) && (bp.size() == 2)) {
//
//                    Compound tpp = (Compound) tp;
//                    Compound bpp = (Compound) bp;
//
//                    if (derivationMatch(tpp.term(1), bpp.term(0))) {
//                        t = td + bd;
//
//                        //chained inner
//                        if (!derivationMatch(cb, bpp.term(1))) {
//                            t = -t; //invert direction
//                        }
//                    } else if (derivationMatch(tpp.term(0), bpp.term(1))) {
//                        //chain outer
//                        t = td + bd; //?? CHECK
//                    } else if (derivationMatch(tpp.term(0), bpp.term(0))) {
//                        //common left
//                        t = td - bd;
//                    } else if (derivationMatch(tpp.term(1), bpp.term(1))) {
//                        //common right
//                        t = bd - td;
//                    } else {
//                        //throw new RuntimeException("unhandled case");
//                        t = (bd + td) / 2; //???
//                    }
//
//                }

                long to = task.start();
                long bo = belief != null ? belief.start() : to;

                int occDiff = (to != ETERNAL && bo != ETERNAL) ? (int) (bo - to) : 0;

                if (td == DTERNAL && bd == DTERNAL) {

//                    int aTask = tp.subtermTime(ca, DTERNAL);
//                    int aBelief = bp.subtermTime(ca, DTERNAL);
//                    int bTask = tp.subtermTime(cb, DTERNAL);
//                    int bBelief = bp.subtermTime(cb, DTERNAL);
//
//                    if (belief != null) {
//
//                        boolean reversed = false;
//                    /* reverse subterms if commutive and the terms are opposite the corresponding pattern */
//                        if (derived.op().commutative) {
//                            if (!derivationMatch(
//                                    p.resolve(((Compound) cp).term(0)),
//                                    derived.term(0))) {
//                                occDiff = -occDiff;
//                                reversed = true;
//                            }
//                        }
//
//
//                        if (aTask != DTERNAL && aBelief == DTERNAL &&
//                                bBelief != DTERNAL && bTask == DTERNAL) {
//                            //forward: task -> belief
//                            //t = (int) (task.occurrence() - belief().occurrence());
//                            t = occDiff;
//                            if (reversed) occ -= t;
//                            else occ += t;
//
//                        } else if (aTask == DTERNAL && aBelief != DTERNAL &&
//                                bBelief == DTERNAL && bTask != DTERNAL) {
//                            //reverse: belief -> task
//                            t = -occDiff;
//                            //t = (int) (belief().occurrence() - task.occurrence());
//                            //t = (int) (task.occurrence() - belief().occurrence());
//
//                            if (!reversed) {
//                                occ -= t;
//                            } else {
//                                occ += t;
//                            }
//
//
//                        } else {
//
//                            //both ITERNAL
//
//                            if ((to != ETERNAL) && (bo != ETERNAL)) {
//                                t = occDiff;
//                                if (reversed) occ -= t;
//                                else occ += t;
//                            }
//
//                        }
//                    }

                } else if (td == DTERNAL) {
                    //belief has dt
                    t = bd;// + occDiff;
                    //TODO align
                } else if (bd == DTERNAL) {
                    //task has dt
                    t = td + occDiff;
                    //occ += t; //TODO check this alignment

                } else {
                    //t = occDiff;
                    //throw new RuntimeException("unhandled case");
                    //???
                    //t = (td+bd)/2;
                }
            }

        }


        //apply occurrence shift
        if (occ > Tense.TIMELESS) {

            Term T = resolve(p, tt);
            if (T!=null) {
                Term B = resolve(p, bb);

                if (belief != null) {
                    //TODO cleanup simplify this is messy and confusing

                    if (task.isEternal() && !belief.isEternal()) {
                        //find relative time of belief in the task, relative time of the conclusion, and subtract
                        //the occ (=belief time's)
                        long timeOfBeliefInTask = T.subtermTime(B, td);
                        long timeOfDerivedInTask = T.subtermTime(derived, td);
                        if (timeOfDerivedInTask != DTERNAL && timeOfBeliefInTask != DTERNAL)
                            occ += (timeOfDerivedInTask - timeOfBeliefInTask);
                        else if (timeOfDerivedInTask != DTERNAL)
                            occ += timeOfDerivedInTask;
                    } else if (!task.isEternal() && belief.isEternal() && B != null) {
                        long timeOfTaskInBelief = B.subtermTime(T, bd);
                        long timeOfDerivedInBelief = B.subtermTime(derived, bd);

                        if (timeOfTaskInBelief != DTERNAL && timeOfDerivedInBelief != DTERNAL)
                            occ += (timeOfDerivedInBelief - timeOfTaskInBelief);
                        else if (timeOfDerivedInBelief != DTERNAL)
                            occ += timeOfDerivedInBelief;
                        else {
                            long timeOfDerivedInTask = T.subtermTime(derived, td);
                            if (timeOfDerivedInTask != DTERNAL) {
                                occ += timeOfDerivedInTask;
                            } else {
                                //??
                            }
                        }
                    } else if (!task.isEternal() && !belief.isEternal()) {
                        //throw new RuntimeException("ambiguous task or belief");

                        //long ot = T.subtermTime(C, td);
                        //long ob = B.subtermTime(C, bd);
                        //if (t!=ITERNAL)
                        //    occ -= t;
                    }
                } else {

                    if (!task.isEternal()) {
                        long timeOfDerivedInTask = T.subtermTime(derived, td);
                        if (timeOfDerivedInTask != DTERNAL)
                            occ += timeOfDerivedInTask;
                    } else {

//                        int ot = tp.subtermTime(cp, td);
//                        int ob = bp.subtermTime(cp, bd);
//
//                        if (ot != DTERNAL) {
//                            if (tp instanceof Compound) {
//                                Compound ctp = (Compound) tp;
//                                if (derivationMatch(ctp.term(0), cp)) {
//                                    ot -= td;
//                                }
//                            }
//                            occ += ot; //occ + ot;
//                        } else if (ob != DTERNAL) {
//
//                            if (belief.start() != task.start()) { //why?
//                                if (bp instanceof Compound) {
//                                    Compound cbp = (Compound) bp;
//                                    if (!derivationMatch(cbp.term(1), cp)) {
//                                        ob -= bd;
//                                    }
//                                }
//                            }
//
//                            occ += ob;
//
//                        } else {
//                            //neither, remain eternal
//                            throw new RuntimeException("unhandled case");
//                        }
                    }
                }
            }


        }
        //}
        //}


        if ((t != DTERNAL) && (t != derived.dt())) {
        /*derived = (Compound) p.premise.nar.memory.index.newTerm(derived.op(), derived.relation(),
                t, derived.subterms());*/

            if (derived.size() == 2 || t == 0)
                derived = dt(derived, t, occReturn);

//            int nt = derived.t();
//            if (occ > TIMELESS) {
//                if (Math.signum(t) != Math.signum(nt)) {
//                    //re-align the occurrence
//                    occ -= t;
//                } else {
//                    occ -= nt;
//                }
//            }
        }

        if (belief!=null) {
            long taskOcc = task.start();
            if (taskOcc!=ETERNAL) {
                long belOcc = belief.start();
                if (belOcc!=ETERNAL) {
                    Interval ii = Interval.union(taskOcc, task.end(), belOcc, belief.end() );
                    if (ii != null) {
                        occReturn[0] = ii.a;
                        occReturn[1] = ii.b;

                        return derived;
                    } else {
                        //no intersection: point-like below
                    }
                }
            }
        } else {
            //inherit task's occurrence exactly
            occReturn[0] = task.start();
            occReturn[1] = task.end();
            return derived;
        }

        occReturn[0] = occ;
        return derived;

    };

    static long occInterpolate(@NotNull Task t, @Nullable Task b) {

        long to = t.start();
        if (b == null) {
            return to;
        }

        long bo = b.start();
        if (t.isQuestOrQuestion() || to==ETERNAL)
            return bo; //use belief time when task is a question, or task is eternal

        if (bo == ETERNAL)
            return to; //dont uneternalize belief, defer to task's occurrence

        //if (to != ETERNAL && bo != ETERNAL) {

        float tw = t.evi();
        float bw = b.evi();
        return Util.lerp((tw) / (bw + tw), to, bo);
//        } else {
//            return bo != ETERNAL ? bo : to;
//        }

    }


    @NotNull
    static Compound dt(@NotNull Compound derived, int dt, long[] occReturn) {
        Op o = derived.op();
        if (!o.temporal) {
            dt = DTERNAL;
        }
//        if (!o.temporal && dt != DTERNAL && dt != 0 && occReturn[0] != ETERNAL) {
//            //something got reduced to a non-temporal, so shift it to the midpoint of what the actual term would have been:
//            occReturn[0] += dt / 2;
//            dt = DTERNAL;
//        }
        if (derived.dt() != dt) {
            TermContainer ds = derived.subterms();
            @NotNull Term n = terms.the(o, dt, ds);
            if (!(n instanceof Compound))
                throw new InvalidTermException(o, dt, ds, "Untemporalizable to new DT");

            //TODO decide if this is correct
            if (derived.op()==CONJ && occReturn[0]!=ETERNAL && occReturn[1] == ETERNAL)
                occReturn[1] = occReturn[0] + n.dtRange();

            return (Compound) n;
        } else {
            return derived;
        }
    }


    public static long occurrenceTarget(Premise p, @NotNull OccurrenceSolver s) {
        long tOcc = p.task.start();
        Task b = p.belief;
        if (b == null) {
            return tOcc;
        } else {
            long bOcc = b.start();
            return s.compute(tOcc, bOcc);

//            //if (bOcc == ETERNAL) {
//            return (tOcc != ETERNAL) ?
//                        whenBothNonEternal.compute(tOcc, bOcc) :
//                        ((bOcc != ETERNAL) ?
//                            bOcc :
//                            ETERNAL
//            );
        }
    }
}
