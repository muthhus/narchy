package nars.concept;

import nars.Global;
import nars.Op;
import nars.Premise;
import nars.nal.meta.PremiseEval;
import nars.nal.op.Derive;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.*;

/**
 * Strategies for solving temporal components of a derivation
 */
@FunctionalInterface
public interface Temporalize {


    static long earlyOrLate(long t, long b, boolean early) {
        boolean tEternal = t == ETERNAL;
        boolean bEternal = b == ETERNAL;
        if (!tEternal && !bEternal) {
            if (early)
                return t <= b ? t : b;
            else
                return t >= b ? t : b;
        } else if (!bEternal && tEternal) {
            return b;
        } else if (!tEternal && bEternal) {
            return t;
        }
        return ETERNAL;
    }

    nars.Premise.OccurrenceSolver latestOccurrence = (t, b) -> earlyOrLate(t, b, false);
    nars.Premise.OccurrenceSolver earliestOccurrence = (t, b) -> earlyOrLate(t, b, true);

    /**
     * @param derived   raw resulting untemporalized derived term that may or may not need temporalized and/or occurrence shifted as part of a derived task
     * @param p         current match context
     * @param d         derivation rule being evaluated
     * @param occReturn holds the occurrence time as a return value for the callee to use in building the task
     * @return
     */
    @NotNull
    Compound compute(@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn);

    /**
     * early-aligned, difference in dt
     */
    Temporalize dtTminB = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, @NotNull long[] occReturn) -> {
        return dtDiff(derived, p, occReturn, +1);
    };
    /**
     * early-aligned, difference in dt
     */
    Temporalize dtBminT = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, @NotNull long[] occReturn) -> {
        return dtDiff(derived, p, occReturn, -1);
    };
    /**
     * early-aligned, difference in dt
     */
    Temporalize dtIntersect = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, @NotNull long[] occReturn) -> {
        return dtDiff(derived, p, occReturn, 0);
    };
    /**
     * early-aligned, difference in dt
     */
    Temporalize dtUnion = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, @NotNull long[] occReturn) -> {
        //TODO
        return dtDiff(derived, p, occReturn, 2);
    };
    Temporalize dtUnionReverse = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, @NotNull long[] occReturn) -> {
        //TODO
        return dtDiff(derived, p, occReturn, -2);
    };

    @NotNull
    static Compound dtDiff(@NotNull Compound derived, @NotNull PremiseEval p, @NotNull long[] occReturn, int polarity) {
        ConceptProcess prem = p.premise;
        Task task = prem.task();
        Compound taskTerm = task.term();
        Termed<Compound> beliefTerm = prem.beliefTerm();

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
            occReturn[0] = prem.occurrenceTarget(earliestOccurrence); //TODO CALCULATE

            //restore forward polarity for function call at the end
            polarity = 1;
        } else {
            //diff
            occReturn[0] = prem.occurrenceTarget(earliestOccurrence);
        }

        return deriveDT(derived, polarity, prem, dt);
    }

    /**
     * simple delta-time between task and belief resulting in the dt of the temporal compound.
     * this assumes the conclusion term is size=2 compound.
     * no occurence shift
     * should be used in combination with a "premise Event" precondition
     */
    Temporalize occForward = (derived, p, d, occReturn) -> {
        return occBeliefMinTask(derived, p, occReturn, +1);
    };
    Temporalize occReverse = (derived, p, d, occReturn) -> {
        return occBeliefMinTask(derived, p, occReturn, -1);
    };


    /**
     * if the premise is an event (and it is allowed to not be) then the dt is the difference
     * in task and belief occurrence times, and the occurrence time is the belief's.
     */
    Temporalize dtIfEvent = (derived, p, d, occReturn) -> {
        if (!p.premise.isEvent()) {
            return derived;
        } else {
            return occBeliefMinTask(derived, p, occReturn, +1);
        }
    };

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
    static Compound occBeliefMinTask(@NotNull Compound derived, @NotNull PremiseEval p, long[] occReturn, int polarity) {
        ConceptProcess prem = p.premise;

        int eventDelta = DTERNAL;

        if (!prem.belief().isEternal() && !prem.task().isEternal()) {
            long earliest = prem.occurrenceTarget(earliestOccurrence);

            //TODO check valid int/long conversion
            eventDelta = (int) (prem.belief().occurrence() -
                    prem.task().occurrence());


            occReturn[0] = earliest;
        }

        //HACK to handle commutive switching so that the dt is relative to the effective subject
        if (derived.op().isCommutative()) {
            if (derived.term(0).equals(prem.belief().term()))
                eventDelta *= -1;
        }

        return deriveDT(derived, polarity, prem, eventDelta);
    }


    /**
     * copiesthe 'dt' and the occurence of the task term directly
     */
    Temporalize dtTaskExact = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn) -> {
        ConceptProcess prem = p.premise;
        return dtExact(derived, occReturn, prem, true);
    };
    Temporalize dtBeliefExact = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn) -> {
        ConceptProcess prem = p.premise;
        return dtExact(derived, occReturn, prem, false);
    };


    /**
     * special handling for dealing with detaching, esp. conjunctions which involve a potential mix of eternal and non-eternal premise components
     */
    @Nullable
    Temporalize decomposeTask = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, @NotNull long[] occReturn) -> {
        return decompose(derived, p, occReturn, true);
    };
    @Nullable
    Temporalize decomposeBelief = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, @NotNull long[] occReturn) -> {
        return decompose(derived, p, occReturn, false);
    };

    @NotNull
    static Compound decompose(@NotNull Compound derived, @NotNull PremiseEval p, @NotNull long[] occReturn, boolean decomposeTask) {
        ConceptProcess prem = p.premise;

        Termed<Compound> decomposed = decomposeTask ? prem.task() : prem.beliefTerm();
        Task premBelief = prem.belief();
        long dOcc = decomposeTask ? prem.task().occurrence() : (premBelief !=null ? premBelief.occurrence() : ETERNAL);

        //if ((tOcc == ETERNAL) && (prem.belief()!=null))
        //    tOcc = prem.belief().occurrence(); //use occurrence from belief


        //assert (decomposed != null);
        //decomposed = prem.beliefTerm();
        //tOcc = ETERNAL;
        //} else {
        //tOcc = ((Task)decomposed).occurrence();
        //}

        Compound decTerm = decomposed.term();
        Compound dtt = decTerm;
        int ddt = dtt.dt();

        Task other = decomposeTask ? premBelief : prem.task();
        Term otherTerm = decomposeTask ? prem.beliefTerm().term() : prem.task().term();

        long oOcc = ETERNAL;
        if (other != null && !other.isEternal()) {
            oOcc = other.occurrence();
        }


        if ((dOcc != ETERNAL) || (oOcc != ETERNAL)) {

            int shift = 0;

            int matchedDerived = -1, matchedOther = -1;

            if (ddt != DTERNAL) {


                //shift to occurrence time of the subterm within the decomposed term's task


                for (int i = 0; i < decTerm.size(); i++) {
                    Term dct = decTerm.term(i);
                    Term rdt = p.resolve(dct);
                    if (rdt==null)
                        continue;

                    if (rdt.equals(derived)) {
                        int st = decTerm.subtermTime(dct);
                        if (st != DTERNAL) {
                            shift += st;
                            matchedDerived = i;
                        }
                    }

                    if (rdt.equals(otherTerm)) {
                        int st = decTerm.subtermTime(dct);
                        if (st != DTERNAL) {
                            shift -= st;
                            matchedOther = i;
                        }
                    }
                }

            }

            //long rOcc = ETERNAL;

            /*if (dOcc == ETERNAL) {
                //rOcc = oOcc;
            }
            else*/ if (dOcc!=ETERNAL && oOcc == ETERNAL) {


                if (shift < 0) {
                    if (((ddt > 0) && (matchedDerived < matchedOther))||
                            ((ddt < 0) && (matchedDerived > matchedOther))){
                        shift *= -1;
                    }
                }

                oOcc = dOcc;
            } /*else if (dOcc!=ETERNAL && oOcc!=ETERNAL && dOcc!=oOcc) {
                //both specify a possible occurrence time to use; base occurrence time according to the most confident
                //float dConf = decomposeTask ? prem.task().conf() : (premBelief !=null ? premBelief.conf() : 0);
                //if (dConf > other.conf())
                    oOcc = (oOcc + dOcc)/2; //TODO interpolate?
            }*/



            if (oOcc!=ETERNAL) {
                occReturn[0] = oOcc + shift;

            }
        }

        return derived;
    }

    @NotNull
    static Compound dtExact(@NotNull Compound derived, @NotNull long[] occReturn, @NotNull ConceptProcess prem, boolean taskOrBelief) {
        Term dtTerm = taskOrBelief ? prem.task().term() : prem.beliefTerm().term();

        Task t = prem.task();
        Task b = prem.belief();
        if (!taskOrBelief && b!=null && b.occurrence()!=ETERNAL) {
            int derivedInT = dtTerm.subtermTime(derived);
            if (derivedInT == DTERNAL && derived.op() == Op.IMPLICATION) {
                //try to find the subtermTime of the implication's subject
                derivedInT = dtTerm.subtermTime( derived.term(0) );
            }
            occReturn[0] = t.occurrence() + derivedInT;
        } else {
            occReturn[0] = t.occurrence(); //the original behavior, but may not be right
        }


        if (derived.op().isTemporal()) {
            int dtdt = dtTerm instanceof Compound ? ((Compound)dtTerm).dt() : DTERNAL;
            return deriveDT(derived, +1, prem, dtdt);
        } else
            return derived;
    }


    /**
     * dt is supplied by Task
     */
    Temporalize dtTask = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, true, false);

    Temporalize dtTaskEnd = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, true, true);

    /**
     * dt is supplied by Belief
     */
    Temporalize dtBelief = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, false, false);
    Temporalize dtBeliefEnd = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, false, true);

    @NotNull
    static Compound dtTaskOrBelief(@NotNull Compound derived, @NotNull PremiseEval p, long[] occReturn, @NotNull Premise.OccurrenceSolver s, boolean taskOrBelief/*, boolean shiftToPredicate*/, boolean end) {
        ConceptProcess premise = p.premise;

        long o = premise.occurrenceTarget(s);
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
                long ddt = premise.task().term().dt();
                if (ddt != DTERNAL)
                    o += ddt;
            } else if (taskOrBelief && !end) {
                //NOTHING NECESSARY
                /*long ddt = premise.task().term().dt();
                if (ddt != DTERNAL)
                    o -= ddt;*/
            } else {

                //Task belief = premise.belief();
                Termed<Compound> beliefTerm = premise.beliefTerm();
                //if (belief != null) {
                long ddt = beliefTerm.term().dt();
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
    static Compound deriveDT(@NotNull Compound derived, int polarity, @NotNull ConceptProcess premise, int eventDelta) {
        if (eventDelta == DTERNAL)
            return derived; //no change

        //exception: if reduction results in || which is non-temporal
        if (derived.op() == Op.DISJUNCTION)
            return derived;

        if (!Global.DEBUG && !derived.op().isTemporal())
            return derived; //disregard dt if not in debug mode

        return derived.dt(eventDelta * polarity);
    }

    /**
     * combine any existant DT's in the premise (assumes both task and belief are present)
     */
    @Nullable
    Temporalize dtCombine = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Derive d, long[] occReturn) -> {
        ConceptProcess premise = p.premise;

        Task task = premise.task();
        int taskDT = task.term().dt();
        Term bt = premise.beliefTerm().term();

        int beliefDT = (bt instanceof Compound) ? ((Compound) bt).dt() : DTERNAL;

        int eventDelta;
        if (taskDT == DTERNAL && beliefDT == DTERNAL) {
            eventDelta = DTERNAL;
        } else if (taskDT != DTERNAL && beliefDT != DTERNAL) {

            Task belief = premise.belief();

            if (belief != null && task.isBeliefOrGoal() && belief.task().isBeliefOrGoal()) {
                //blend task and belief's DT's weighted by their relative confidence
                float taskConf = task.conf();
                eventDelta = Math.round(Util.lerp(
                        taskDT,
                        beliefDT,
                        taskConf / (taskConf + belief.conf())
                ));
            } else {
                eventDelta = taskDT;
            }

        } else if (taskDT == DTERNAL) {
            eventDelta = beliefDT;
        } else /*if (beliefDT == ITERNAL)*/ {
            eventDelta = taskDT;
        }

        occReturn[0] = premise.occurrenceTarget(earliestOccurrence); //(t, b) -> t >= b ? t : b); //latest occurring one

        return deriveDT(derived, 1, premise, eventDelta);
    };

    Temporalize AutoSimple = (derived, p, d, occReturn) -> {

        ConceptProcess premise = p.premise;

        long occ = premise.occurrenceTarget((t, b) -> t); //reset

        occReturn[0] = occ;

        return derived;
    };

    /**
     * "automatic" implementation of Temporalize, used by default. slow and wrong about 25..30% of the time sux needs rewritten or replaced
     * apply temporal characteristics to a newly derived term according to the premise's
     */
    @Nullable
    Temporalize Auto = (derived, p, d, occReturn) -> {


        Term tp = d.rule.getTask();
        Term bp = d.rule.getBelief();

        ConceptProcess premise = p.premise;

        Task task = premise.task();
        Task belief = premise.belief();

        long occ = premise.occurrenceTarget((t, b) -> t); //reset

        Compound tt = task.term();
        Term bb = premise.beliefTerm().term(); // belief() != null ? belief().term() : null;

        int td = tt.dt();
        int bd = bb instanceof Compound ? ((Compound) bb).dt() : DTERNAL;

        int t = DTERNAL;

        Term cp = d.conclusionPattern; //TODO this may be a wrapped immediatefunction?

        if (derived.op().isTemporal() && cp instanceof Compound) {

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

                    if (tpp.term(1).equals(bpp.term(0))) {
                        t = td + bd;

                        //chained inner
                        if (!cb.equals(bpp.term(1))) {
                            t = -t; //invert direction
                        }
                    } else if (tpp.term(0).equals(bpp.term(1))) {
                        //chain outer
                        t = td + bd; //?? CHECK
                    } else if (tpp.term(0).equals(bpp.term(0))) {
                        //common left
                        t = td - bd;
                    } else if (tpp.term(1).equals(bpp.term(1))) {
                        //common right
                        t = bd - td;
                    } else {
                        //throw new RuntimeException("unhandled case");
                        t = (bd + td) / 2; //???
                    }

                }

                long to = task.occurrence();
                long bo = belief != null ? belief.occurrence() : ETERNAL;

                int occDiff = (to != ETERNAL && bo != ETERNAL) ? (int) (bo - to) : 0;

                if (td == DTERNAL && bd == DTERNAL) {

                    int aTask = tp.subtermTime(ca, DTERNAL);
                    int aBelief = bp.subtermTime(ca, DTERNAL);
                    int bTask = tp.subtermTime(cb, DTERNAL);
                    int bBelief = bp.subtermTime(cb, DTERNAL);

                    if (belief != null) {

                        boolean reversed = false;
                    /* reverse subterms if commutive and the terms are opposite the corresponding pattern */
                        if (derived.op().isCommutative()) {
                            if (!p.resolve(((Compound) cp).term(0)).equals(derived.term(0))) {
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
        if (occ > TIMELESS) {

            Term T = p.resolve(tt);
            Term B = bb != null ? p.resolve(bb) : null;
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
                } else if (!task.isEternal() && belief.isEternal()) {
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
                            if (ctp.term(0).equals(cp)) {
                                ot -= td;
                            }
                        }
                        occ += ot; //occ + ot;
                    } else if (ob != DTERNAL) {

                        if (belief.occurrence() != task.occurrence()) { //why?
                            if (bp instanceof Compound) {
                                Compound cbp = (Compound) bp;
                                if (!cbp.term(1).equals(cp)) {
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
        //}
        //}

        if (t != DTERNAL) {
        /*derived = (Compound) p.premise.nar.memory.index.newTerm(derived.op(), derived.relation(),
                t, derived.subterms());*/

            derived = derived.dt(t);

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
        //return nar.memory.index.transformRoot(derived, temporalize);

    };
}
