package nars.time;

import jcog.Util;
import nars.$;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.nal.meta.Conclude;
import nars.nal.meta.Derivation;
import nars.nal.meta.OccurrenceSolver;
import nars.nal.rule.PremiseRule;
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
        } else if (!bEternal/* && tEternal*/) {
            return b;
        } else if (!tEternal/* && bEternal*/) {
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
    @Nullable TimeFunctions dternal = (derived, p, d, occReturn, confScale) -> dt(derived, DTERNAL, p, occReturn);

    @NotNull
    static Compound dtDiff(@NotNull Compound derived, @NotNull Derivation p, @NotNull long[] occReturn, int polarity) {

        Compound taskTerm = (Compound) p.taskTerm.unneg();
        Termed<Compound> beliefTerm = p.beliefTerm;

        int dt;
        int ttd = taskTerm.dt();
        int btd = beliefTerm.term().dt();
        if (ttd != DTERNAL && btd != DTERNAL) {
            switch (polarity) {
                case 0:
                    dt = (ttd + btd) / 2; //intersect: 0
                    break;
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
            occReturn[0] = p.occurrenceTarget(earliestOccurrence); //TODO CALCULATE

            //restore forward polarity for function call at the end
            polarity = 1;
        } else {
            //diff
            occReturn[0] = p.occurrenceTarget(earliestOccurrence);
        }

        return deriveDT(derived, polarity, p, dt, occReturn);
    }

    /**
     * simple delta-time between task and belief resulting in the dt of the temporal compound.
     * this assumes the conclusion term is size=2 compound.
     * no occurence shift
     * should be used in combination with a "premise Event" precondition
     */
    TimeFunctions occForward = (derived, p, d, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, +1);
    TimeFunctions occReverse = (derived, p, d, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, -1);


//    /**
//     * if the premise is an event (and it is allowed to not be) then the dt is the difference
//     * in task and belief occurrence times, and the occurrence time is the belief's.
//     */
//    Temporalize dtIfEvent = (derived, p, d, occReturn) -> {
//        if (!p.premise.isEvent()) {
//            return derived;
//        } else {
//            return occBeliefMinTask(derived, p, occReturn, +1);
//        }
//    };

//    /** shifts to task's predicate by the task's dt (if present) */
//    Temporalize taskPredicate = (derived, p, d, occReturn) -> {
//        long oc = p.premise.occurrenceTarget(latestOccurrence);
//        int tdt = p.premise.task().term().dt();
//        if (tdt!=ITERNAL)
//            oc += tdt;
//        occReturn[0] = oc;
//        return derived;
//    };

    @NotNull
    static Compound occBeliefMinTask(@NotNull Compound derived, @NotNull Derivation p, @NotNull long[] occReturn, int polarity) {

        int dt;

        long beliefStart = p.belief.start();
        long taskStart = p.task.start();

        if (beliefStart != ETERNAL && taskStart != ETERNAL) {

            dt = (int) (beliefStart - taskStart); //TODO check valid int/long conversion
            long start = p.occurrenceTarget(earliestOccurrence);

//            long beliefEnd = p.belief.end();
//            long taskEnd = p.task.end();
//
//            long start;
//
//            if (beliefEnd == beliefStart && taskEnd == taskStart) {
//
//                //HACK the original calculation, involving point events
//
//                dt = (int) (beliefStart - taskStart); //TODO check valid int/long conversion
//                start = p.occurrenceTarget(earliestOccurrence);
//
//
//            } else {
//
////                switch (derived.op()) {
////                    case CONJ:
//                        Interval i = Interval.union(taskStart, taskEnd, beliefStart, beliefEnd);
//                        start = i.a;
//                        dt = (int) i.length();
////                        break;
////                    default:
////                        dt = (int) (beliefStart - taskStart); //TODO check valid int/long conversion
////                        start = p.occurrenceTarget(earliestOccurrence);
////                        break;
//
////                }
//            }
//
//

            occReturn[0] = start;

            //HACK to handle commutive switching so that the dt is relative to the effective subject
            if (dt != 0 && dt != DTERNAL && derived.op().commutative) {

                Term bt = p.beliefTerm;
                Term d0 = derived.term(0);

                if (derivationMatch(bt, d0))
                    dt *= -1;
            }


            return deriveDT(derived, polarity, p, dt, occReturn);


        } else if (beliefStart != ETERNAL) {
            occReturn[0] = beliefStart;
            //eventDelta = DTERNAL;
        } else if (taskStart != ETERNAL) {
            occReturn[0] = taskStart;
            //eventDelta = DTERNAL;
        } else {
            //eventDelta = DTERNAL;
        }

        return derived;
    }

    @Deprecated
    static boolean derivationMatch(@NotNull Term a, @NotNull Term b) {
        return /*productNormalize*/(a.unneg()).equalsIgnoringVariables(/*productNormalize*/b);
    }

    static boolean derivationMatch(@NotNull Term a, @NotNull Term b, @NotNull Derivation p) {
        Term pa = resolve(p, a);
        if (pa!=null) {
            Term pb = resolve(p, b);
            if (pb!=null) {
                return pa.unneg().equalsIgnoringVariables(pb);
            }
        }
        return false;
    }


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
            return decompose(derived, p, occReturn, true);
            //}
        } else {
            occReturn[0] = task.occurrence();

            //3 or more
            if (derived.op() == CONJ) {
                int dt = task.dt();
                //in this case, set the resulting conjunction to either the && or &| state, copying from the task's term
                return deriveDT(derived, +0, p, dt, occReturn);
            }

        }

        return derived;
    };

    @Nullable
    static Compound decompose(@NotNull Compound derived, @NotNull Derivation p, @NotNull long[] occReturn, boolean decomposeTask) {


        Task task = p.task;
        Task belief = p.belief;
        //if (premBelief == null)
        //premBelief = p.task; //it is the task itself being decomposed

        //Task decomposingTask = (decomposeTask) ? task : belief;
        Task otherTask = (decomposeTask) ? belief : task;

        Compound decomposedTerm = (Compound) (decomposeTask ? p.taskTerm : p.beliefTerm);
        //int dtDecomposed = decomposedTerm.dt();

        long occDecomposed = decomposeTask ? task.occurrence() : (belief != null ? belief.occurrence() : task.occurrence());

        //the non-decomposed counterpart of the premise


        long occOther = (otherTask != null) ? otherTask.occurrence() : ETERNAL;


        if ((occDecomposed == ETERNAL) && (occOther == ETERNAL)) {
            //no temporal basis that can apply. only derive an eternal result
            // if there is no actual temporal relation in the decomposition

            int ddt = decomposedTerm.dt();
            if ((ddt == 0 || ddt == DTERNAL || ddt == XTERNAL)) {
                return derived; //allow eternal decompose since no temporal information is inside the decomposed term
            } else {
                return noTemporalBasis(derived);
            }

        } else {

            //project any ETERNAL to NOW, since an ETERNAL truth is the same for the present moment
//            if (occDecomposed == ETERNAL)
//                //occDecomposed = p.time();
//                occDecomposed = occOther;
//            if (occOther == ETERNAL)
//                //occOther = p.time();
//                occOther = occDecomposed;

            long occ = ETERNAL;

            //int edtDecomposed = dtDecomposed != DTERNAL ? dtDecomposed : 0; //effective dt decomposed

            if (decomposedTerm.size() != 2) {
                //probably a (&&+0, ...)
                occ = occDecomposed != ETERNAL ? occDecomposed : occOther;
            } else {

                @Nullable Term rDecomposed = resolve(p, decomposedTerm);

                @Nullable Term rDerived = resolve(p, derived);

                if (rDecomposed!=null && rDerived!=null && (occDecomposed != ETERNAL)) {

                    int dt = rDecomposed.subtermTime(rDerived);
                    if (dt != DTERNAL)
                        occ = occDecomposed + dt;

                }

                if (occ == ETERNAL && occOther != ETERNAL) {

                    @Nullable Term rOtherTerm = resolve(p, decomposeTask ? p.beliefTerm.term() : p.taskTerm);
                    if (rOtherTerm!=null) {

                        if (derivationMatch(rOtherTerm, derived, p)) {
                            occ = occOther;
                        } else if (rDecomposed != null) {
                            int otherInDecomposed = rDecomposed.subtermTime(rOtherTerm);
                            if (decomposedTerm.dt() == 0 && otherInDecomposed == 0) {
                                //special case for &&+0 having undergone some unrecognizable change
                                occ = occOther - otherInDecomposed; //+0 should ensure it has the same time as this siblign event

                            } else if (rDerived != null) { //{ && otherInDecomposed != DTERNAL) {
                                int derivedInDecomposed = rDecomposed.subtermTime(rDerived);
                                if (derivedInDecomposed != DTERNAL) {
                                    occ = occOther + derivedInDecomposed;
                                    if (otherInDecomposed!=DTERNAL) //???
                                        occ -= otherInDecomposed;


                                }
                            }

                        }
                    }

                }



                if (occ == ETERNAL) {
                    return noTemporalBasis(derived);
                }

            }


            occReturn[0] = occ;

            return derived;

        }

    }

    @Nullable
    static Term resolve(@NotNull Derivation p, @NotNull Term t) {
        return p.resolve($.terms.productNormalize(t));
        //return r!=null ? r) : null;
    }

    @Nullable
    static Compound noTemporalBasis(@NotNull Compound derived) {
        if (Param.DEBUG_EXTRA)
            throw new InvalidTermException(derived.op(), derived.dt(), derived.terms(),
                    "no basis for relating other occurrence to derived");
        else
            return null;
    }


//        int shift = 0;
//        int matchedDerived = -1, matchedOther = -1;
//
//        if (dtDecomposed != DTERNAL) {
//
//
//            //shift to occurrence time of the subterm within the decomposed term's task
//
//
//            for (int i = 0; i < decomposedTerm.size(); i++) {
//                Term dct = decomposedTerm.term(i);
//                Term rdt = p.resolve(dct);
//                if (rdt==null)
//                    continue;
//
//                if (rdt.equals(derived)) {
//                    int st = decomposedTerm.subtermTime(dct);
//                    if (st != DTERNAL) {
//                        shift += st;
//                        matchedDerived = i;
//                    }
//                }
//
//                if (rdt.equals(otherTerm)) {
//                    int st = decomposedTerm.subtermTime(dct);
//                    if (st != DTERNAL) {
//                        shift -= st;
//                        matchedOther = i;
//                    }
//                }
//            }
//
//        }
//
////        long occ;
////        //long rOcc = ETERNAL;
////
////            /*if (dOcc == ETERNAL) {
////                //rOcc = oOcc;
////            }
////
////            else*/
////        if (occOther!=ETERNAL && occDecomposed!=ETERNAL) {
////            //conflict, use dOcc by default
////            occ = occDecomposed;
////        }
////        else if (occOther != ETERNAL && occDecomposed == ETERNAL) {
////            occ = occOther;
////        }
////        else if (occDecomposed != ETERNAL && occOther == ETERNAL) {
////
////
////            if (shift < 0) {
////                if (((dtDecomposed > 0) && (matchedDerived < matchedOther))||
////                        ((dtDecomposed < 0) && (matchedDerived > matchedOther))){
////                    shift *= -1;
////                }
////            }
////
////            occ = occDecomposed;
////        } else {
////            //should not happen, prevented by condition above
////            occ = ETERNAL;
////            shift = 0;
////        }
////
////            /*else if (dOcc!=ETERNAL && oOcc!=ETERNAL && dOcc!=oOcc) {
////                //both specify a possible occurrence time to use; base occurrence time according to the most confident
////                //float dConf = decomposeTask ? prem.task().conf() : (premBelief !=null ? premBelief.conf() : 0);
////                //if (dConf > other.conf())
////                    oOcc = (oOcc + dOcc)/2; //TODO interpolate?
////            }*/
////
////
////        if (occOther!=ETERNAL) {
////            occReturn[0] = occ + shift;
////
////        }
//
//        return derived;
//    }

    @NotNull
    static Compound dtExact(@NotNull Compound derived, @NotNull long[] occReturn, @NotNull Derivation p, boolean taskOrBelief) {

        Term dtTerm = taskOrBelief ? p.taskTerm.unneg() : p.beliefTerm;

        Task t = p.task;
        Task b = p.belief;

        long tOcc = t.occurrence();
        if (b!=null) {
            if (t.isQuestOrQuestion() || tOcc==ETERNAL)
                tOcc = b.occurrence(); //use belief time when task is a question, or task is eternal
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


        if (dtTerm instanceof Compound) {
            int dtdt = ((Compound) dtTerm).dt();
            return deriveDT(derived, +1, p, dtdt, occReturn);
        } else
            return derived;
    }


    /**
     * dt is supplied by Task
     */
    TimeFunctions dtTask = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, true, false);

    TimeFunctions dtTaskEnd = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, true, true);

    /**
     * dt is supplied by Belief
     */
    TimeFunctions dtBelief = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, false, false);
    TimeFunctions dtBeliefEnd = (@NotNull Compound derived, @NotNull Derivation p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, false, true);

    @NotNull
    static Compound dtTaskOrBelief(@NotNull Compound derived, @NotNull Derivation p, long[] occReturn, @NotNull OccurrenceSolver s, boolean taskOrBelief/*, boolean shiftToPredicate*/, boolean end) {

        long o = p.occurrenceTarget(s);
//        if (o != ETERNAL) {
//            long taskDT;
//            if (taskOrBelief) {
//                taskDT = premise.task().term().dt();
//            } else {
//                taskDT = premise.belief().term().dt();
//            }
//            if (taskDT != DTERNAL) {
//                if (end && taskDT > 0) {
//                    o += taskDT;
//                } else if (!end && taskDT < 0) {
//                    o -= taskDT;
//                }
//            }
//        }
        if (o != ETERNAL) {
            if (taskOrBelief && end) {
                //long taskDT = (taskOrBelief ? premise.task() : premise.belief()).term().dt();

                long ddt = ((Compound) p.taskTerm.unneg()).dt();
                if (ddt != DTERNAL)
                    o += ddt;
            } else if (taskOrBelief && !end) {
                //NOTHING NECESSARY
                /*long ddt = premise.task().term().dt();
                if (ddt != DTERNAL)
                    o -= ddt;*/
            } else {

                //Task belief = premise.belief();
                Compound beliefTerm = (Compound) p.beliefTerm;
                //if (belief != null) {
                long ddt = beliefTerm.dt();
                if (ddt != DTERNAL) {
                    if (!taskOrBelief && !end) {
                        o -= ddt;
                    } else if (!taskOrBelief && end) {
                        o += ddt;
                    }
                }
                //}
            }

        }

        occReturn[0] = o;

        return derived;
    }

    @NotNull
    static Compound deriveDT(@NotNull Compound derived, int polarity, @NotNull Derivation p, int eventDelta, @NotNull long[] occReturn) {
        int dt;

        dt = eventDelta == DTERNAL ? DTERNAL : eventDelta * polarity;

        return dt(derived, dt, p, occReturn);
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

        occReturn[0] = p.occurrenceTarget(earliestOccurrence);

        if (pre && derived.term(0) instanceof Compound) {
            //set subterm 0's DT


            Compound preSub = (Compound) derived.term(0);
            boolean neg = preSub.op()==NEG;
            if (neg) {
                preSub = (Compound) preSub.unneg(); //unwrap
            }

            Term newPresub = terms.the( preSub,
                ((taskDT != DTERNAL) && (beliefDT != DTERNAL)) ? (taskDT - beliefDT) : DTERNAL
            );

            if (!(newPresub instanceof Compound))
                return null;

            derived = (Compound) terms.the(derived,
                    $.negIf(newPresub, neg),
                    derived.term(1) );
        }
        if (post && derived.term(1) instanceof Compound) {
            //set subterm 1's DT
            Term newSubterm1 = terms.the((Compound) derived.term(1),
                    ((taskDT != DTERNAL) && (beliefDT != DTERNAL)) ? (taskDT - beliefDT) : DTERNAL);

            if (isTrueOrFalse(newSubterm1))
                return null;

            Term newDerived = terms.the(derived, derived.term(0), newSubterm1);
            if (!(newDerived instanceof Compound))
                return null;

            derived = (Compound) newDerived;

        }

        return deriveDT(derived, 1, p, eventDelta, occReturn);
    }

    TimeFunctions occMerge = (derived, p, d, occReturn, confScale) -> {
//        long taskOcc = p.task.occurrence();
//        long beliefOcc = p.belief.occurrence();
//        if (taskOcc == Tense.ETERNAL) {
//            occReturn[0] = beliefOcc;
//        } else if (beliefOcc == Tense.ETERNAL) {
//            occReturn[0] = taskOcc;
//        } else {
//            //merge in proportion to their conf
//            //double tConf = p.task.confWeight();
//            //double bConf = p.belief.confWeight();
//            //double newOcc = Util.lerp(taskOcc, beliefOcc, tConf / (bConf + tConf));
//
//            ;
//        }
        occReturn[0] =
                //chooseByConf(p.task, p.belief, p).occurrence();
                occInterpolate(p.task, p.belief, p);

        return derived;
    };

//    Temporalize AutoSimple = (derived, p, d, occReturn) -> {
//
//        ConceptProcess premise = p.premise;
//
//        long occ = premise.occurrenceTarget((t, b) -> t); //reset
//
//        occReturn[0] = occ;
//
//        return derived;
//    };


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

        long occ = occInterpolate(task, belief, p); //reset

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
                if (td != DTERNAL && bd != DTERNAL && (tp.size() == 2) && (bp.size() == 2)) {
                    Compound tpp = (Compound) tp;
                    Compound bpp = (Compound) bp;

                    if (derivationMatch(tpp.term(1), bpp.term(0))) {
                        t = td + bd;

                        //chained inner
                        if (!derivationMatch(cb, bpp.term(1))) {
                            t = -t; //invert direction
                        }
                    } else if (derivationMatch(tpp.term(0), bpp.term(1))) {
                        //chain outer
                        t = td + bd; //?? CHECK
                    } else if (derivationMatch(tpp.term(0), bpp.term(0))) {
                        //common left
                        t = td - bd;
                    } else if (derivationMatch(tpp.term(1), bpp.term(1))) {
                        //common right
                        t = bd - td;
                    } else {
                        //throw new RuntimeException("unhandled case");
                        t = (bd + td) / 2; //???
                    }

                }

                long to = task.occurrence();
                long bo = belief != null ? belief.occurrence() : to;

                int occDiff = (to != ETERNAL && bo != ETERNAL) ? (int) (bo - to) : 0;

                if (td == DTERNAL && bd == DTERNAL) {

                    int aTask = tp.subtermTime(ca, DTERNAL);
                    int aBelief = bp.subtermTime(ca, DTERNAL);
                    int bTask = tp.subtermTime(cb, DTERNAL);
                    int bBelief = bp.subtermTime(cb, DTERNAL);

                    if (belief != null) {

                        boolean reversed = false;
                    /* reverse subterms if commutive and the terms are opposite the corresponding pattern */
                        if (derived.op().commutative) {
                            if (!derivationMatch(
                                    p.resolve(((Compound) cp).term(0)),
                                    derived.term(0))) {
                                occDiff = -occDiff;
                                reversed = true;
                            }
                        }


                        if (aTask != DTERNAL && aBelief == DTERNAL &&
                                bBelief != DTERNAL && bTask == DTERNAL) {
                            //forward: task -> belief
                            //t = (int) (task.occurrence() - belief().occurrence());
                            t = occDiff;
                            if (reversed) occ -= t;
                            else occ += t;

                        } else if (aTask == DTERNAL && aBelief != DTERNAL &&
                                bBelief == DTERNAL && bTask != DTERNAL) {
                            //reverse: belief -> task
                            t = -occDiff;
                            //t = (int) (belief().occurrence() - task.occurrence());
                            //t = (int) (task.occurrence() - belief().occurrence());

                            if (!reversed) {
                                occ -= t;
                            } else {
                                occ += t;
                            }


                        } else {

                            //both ITERNAL

                            if ((to != ETERNAL) && (bo != ETERNAL)) {
                                t = occDiff;
                                if (reversed) occ -= t;
                                else occ += t;
                            }

                        }
                    }

                } else if (td == DTERNAL && bd != DTERNAL) {
                    //belief has dt
                    t = bd;// + occDiff;
                    //TODO align
                } else if (td != DTERNAL && bd == DTERNAL) {
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


//            if (t!=ITERNAL) {
//                long ot = taskPattern.subtermTime(cp, t);
//                long ob = beliefPattern.subtermTime(cp, t);
//
//                System.out.println(ot + " " + ob);
//
//            }

            //System.out.println(derived + " " + a + ":"+ aTask + "|" + aBelief + ", " + b + ":" + bTask + "|" + bBelief);


        }


        //apply occurrence shift
        if (occ > Tense.TIMELESS) {

            Term T = resolve(p, tt);
            if (T!=null) {
                Term B = resolve(p, bb);
                Term C = derived;

                if (belief != null) {
                    //TODO cleanup simplify this is messy and confusing

                    if (task.isEternal() && !belief.isEternal()) {
                        //find relative time of belief in the task, relative time of the conclusion, and subtract
                        //the occ (=belief time's)
                        long timeOfBeliefInTask = T.subtermTime(B, td);
                        long timeOfDerivedInTask = T.subtermTime(C, td);
                        if (timeOfDerivedInTask != DTERNAL && timeOfBeliefInTask != DTERNAL)
                            occ += (timeOfDerivedInTask - timeOfBeliefInTask);
                        else if (timeOfDerivedInTask != DTERNAL)
                            occ += timeOfDerivedInTask;
                    } else if (!task.isEternal() && belief.isEternal() && B != null) {
                        long timeOfTaskInBelief = B.subtermTime(T, bd);
                        long timeOfDerivedInBelief = B.subtermTime(C, bd);

                        if (timeOfTaskInBelief != DTERNAL && timeOfDerivedInBelief != DTERNAL)
                            occ += (timeOfDerivedInBelief - timeOfTaskInBelief);
                        else if (timeOfDerivedInBelief != DTERNAL)
                            occ += timeOfDerivedInBelief;
                        else {
                            long timeOfDerivedInTask = T.subtermTime(C, td);
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
                        long timeOfDerivedInTask = T.subtermTime(C, td);
                        if (timeOfDerivedInTask != DTERNAL)
                            occ += timeOfDerivedInTask;
                    } else {

                        int ot = tp.subtermTime(cp, td);
                        int ob = bp.subtermTime(cp, bd);

                        if (ot != DTERNAL) {
                            if (tp instanceof Compound) {
                                Compound ctp = (Compound) tp;
                                if (derivationMatch(ctp.term(0), cp)) {
                                    ot -= td;
                                }
                            }
                            occ += ot; //occ + ot;
                        } else if (ob != DTERNAL) {

                            if (belief.occurrence() != task.occurrence()) { //why?
                                if (bp instanceof Compound) {
                                    Compound cbp = (Compound) bp;
                                    if (!derivationMatch(cbp.term(1), cp)) {
                                        ob -= bd;
                                    }
                                }
                            }

                            occ += ob;

                        } else {
                            //neither, remain eternal
                            throw new RuntimeException("unhandled case");
                        }
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
                derived = dt(derived, t, p, occReturn);

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


        occReturn[0] = occ;


        return derived;

    };

    static long occInterpolate(@NotNull Task t, @Nullable Task b, Derivation p) {

        long to = t.occurrence();
        if (b == null) {
            return to;
        }

        long bo = b.occurrence();
        if (t.isQuestOrQuestion() || to==ETERNAL)
            return bo; //use belief time when task is a question, or task is eternal

        if (bo == ETERNAL)
            return to; //dont uneternalize belief, defer to task's occurrence

        //if (to != ETERNAL && bo != ETERNAL) {

        float tw = t.evi();
        float bw = b.evi();
        return Util.lerp(to, bo, (tw) / (bw + tw));
//        } else {
//            return bo != ETERNAL ? bo : to;
//        }

    }


    @NotNull
    static Compound dt(@NotNull Compound derived, int dt, @NotNull Derivation p, long[] occReturn) {
        Op o = derived.op();
        if (!o.temporal) {
            dt = DTERNAL;
        }
        if (!o.temporal && dt != DTERNAL && dt != 0 && occReturn[0] != ETERNAL) {
            //something got reduced to a non-temporal, so shift it to the midpoint of what the actual term would have been:
            occReturn[0] += dt / 2;
            dt = DTERNAL;
        }
        if (derived.dt() != dt) {
            TermContainer ds = derived.subterms();
            @NotNull Term n = terms.the(o, dt, ds);
            if (!(n instanceof Compound))
                throw new InvalidTermException(o, dt, ds, "Untemporalizable to new DT");
            return (Compound) n;
        } else {
            return derived;
        }
    }
}
