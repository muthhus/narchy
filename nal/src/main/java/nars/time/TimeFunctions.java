//package nars.time;
//
//import jcog.Util;
//import jcog.math.Interval;
//import nars.$;
//import nars.Op;
//import nars.Param;
//import nars.Task;
//import nars.control.Derivation;
//import nars.derive.OccurrenceSolver;
//import nars.term.Compound;
//import nars.term.InvalidTermException;
//import nars.term.Term;
//import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
//import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//
//import static nars.Op.*;
//import static nars.task.Revision.chooseByConf;
//import static nars.term.Terms.compoundOrNull;
//import static nars.time.Tense.*;
//
///**
// * Strategies for solving temporal components of a derivation
// */
//@FunctionalInterface
//public interface TimeFunctions {
//
//    /**
//     * @param derived   raw resulting untemporalized derived term that may or may not need temporalized and/or occurrence shifted as part of a derived task
//     * @param p         current match context
//     * @param occReturn holds the occurrence time as a return value for the callee to use in building the task
//     * @param confScale
//     * @return
//     */
//    Term compute(Term derived, @NotNull Derivation p, @NotNull long[] occReturn, @NotNull float[] confScale);
//
//
//    /**
//     * this duplicates most of what is in decomposeBeliefLate, TODO merge them
//     */
//    static void shiftIfImmediate(@NotNull Derivation p, @NotNull long[] occReturn, Term derived) {
//
//        if (derived.op() == CONJ && occReturn[0] != ETERNAL)
//            occReturn[1] = occReturn[0] + derived.dtRange();
//
//        if (occReturn[1] == ETERNAL) occReturn[1] = occReturn[0];
//
//        if (p.task.isGoal()) {
//            long taskStart = p.task.start();
//
//
//            //dont derive a past-tense goal (before the task)
//            if (taskStart != ETERNAL) {
//                long now = p.nar.time();
//
//                assert (occReturn[1] != ETERNAL);
//
//                //occReturn[1] = occReturn[0]; //HACK
//
//                if (taskStart > occReturn[0]) {
//
//                    long range = Math.max(occReturn[1] - occReturn[0], p.task.dtRange());
//
//                    occReturn[0] = taskStart;
//                    occReturn[1] = taskStart + range;
//                }
//            }
//        }
//    }
//
//
//    static long earlyOrLate(long t, long b, boolean early) {
//        boolean tEternal = t == ETERNAL;
//        boolean bEternal = b == ETERNAL;
//        if (!tEternal && !bEternal) {
//            if (early)
//                return t <= b ? t : b;
//            else
//                return t >= b ? t : b;
//        }
//        if (!bEternal/* && tEternal*/) {
//            return b;
//        }
//        if (!tEternal/* && bEternal*/) {
//            return t;
//        }
//        return ETERNAL;
//    }
//
//    //nars.Premise.OccurrenceSolver latestOccurrence = (t, b) -> earlyOrLate(t, b, false);
//    OccurrenceSolver earliestOccurrence = (t, b) -> earlyOrLate(t, b, true);
//    OccurrenceSolver latestOccurence = (t, b) -> earlyOrLate(t, b, false);
//
//    /**
//     * early-aligned, difference in dt
//     */
//    TimeFunctions dtTminB = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, +1);
//    /**
//     * early-aligned, difference in dt
//     */
//    TimeFunctions dtBminT = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, -1);
//    /**
//     * early-aligned, difference in dt
//     */
//    TimeFunctions dtIntersect = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, 0);
//
//    /**
//     * early-aligned, difference in dt
//     */
//    TimeFunctions dtSum = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) -> {
//        return dtDiff(derived, p, occReturn, 2);
//    };
//    TimeFunctions dtSumReverse = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) -> {
//        return dtDiff(derived, p, occReturn, -2);
//    };
//
//
//    /**
//     * does nothing but set DTternal; the input tasks will be considered to be eternal
//     */
//    @Nullable TimeFunctions dternal = (derived, p, occReturn, confScale) -> dt(derived, DTERNAL, occReturn, p);
//
//    @Nullable
//    static Term dtDiff(@NotNull Term derived, @NotNull Derivation p, @NotNull long[] occReturn, int polarity) {
//
//        Term taskTerm = p.taskTerm;
//        Term beliefTerm = p.beliefTerm;
//
//        int dt;
//        int ttd = taskTerm.dt();
//        int btd = beliefTerm instanceof Compound ? ((Compound) beliefTerm).dt() : DTERNAL;
//
//        if (ttd == XTERNAL)
//            ttd = DTERNAL; //HACK
//        if (btd == XTERNAL)
//            btd = DTERNAL; //HACK
//
//        if (ttd != DTERNAL && btd != DTERNAL) {
//            switch (polarity) {
//                case -2:
//                    dt = -(ttd + btd); //sumReverse
//                    break;
//                case 2:
//                    dt = (ttd + btd); //sum
//                    break;
//
//                case -1:
//                case +1: //difference: -1 or +1
//                    dt = (ttd - btd);
//
////                    if (derived.op().commutative) {
////                        boolean lex = derived.term(0).compareTo(derived.term(1)) > 0;
////                        if ((lex && dt < 0) || (!lex && dt > 0)) {
////                            //swap term order
////                            //derived = (Compound) p.index.the(derived, derived.term(1), derived.term(0));
////                            dt = -dt;
////                        }
////                    }
//                    break;
//
//                case 0:
////                    dt = (ttd + btd) / 2; //intersect: 0
////                    break;
//                default:
//                    throw new UnsupportedOperationException();
//            }
//        } else if (ttd != DTERNAL) {
//            dt = ttd;
//        } else if (btd != DTERNAL) {
//            dt = btd;
//        } else {
//            dt = DTERNAL;
//        }
//
//        if ((polarity == 0) || (polarity == 2) || (polarity == -2)) {
//            occReturn[0] = occurrenceTarget(p, earliestOccurrence); //TODO CALCULATE
//
//            polarity = 1;
//        } else {
//            //diff
//            occReturn[0] = occurrenceTarget(p, earliestOccurrence);
//        }
//
//        return deriveDT(derived, polarity, dt, occReturn, p);
//    }
//
//    /**
//     * simple delta-time between task and belief resulting in the dt of the temporal compound.
//     * this assumes the conclusion term is size=2 compound.
//     * no occurence shift
//     * should be used in combination with a "premise Event" precondition
//     */
//    TimeFunctions occForward = (derived, p, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, +1, false);
//    //TimeFunctions occForwardMerge = (derived, p, d, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, +1, true);
//    TimeFunctions occReverse = (derived, p, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, -1, false);
//
//
//    @Nullable
//    static Term occBeliefMinTask(@NotNull Term derived, @NotNull Derivation p, @NotNull long[] occReturn, int polarity, boolean merge) {
//
//        long taskStart = p.task.start();
//        long beliefStart = p.belief.start();
//
//        if (beliefStart != ETERNAL && taskStart != ETERNAL) {
//
//            long beliefEnd = p.belief.end();
//            long taskEnd = p.task.end();
//
//            Interval union = Interval.union(taskStart, taskEnd, beliefStart, beliefEnd);
//            int dt = (int) (beliefEnd - taskStart); //TODO check valid int/long conversion
//
//            occReturn[0] = union.a;
//
//            if (/*merge*/derived.op() == CONJ) {
//
//                occReturn[1] = union.b;
//
//                //merge all events
//                derived = merge(derived.sub(0), taskStart, derived.sub(1), beliefStart);
//
//            } else {
//
//                //HACK to handle commutive switching so that the dt is relative to the effective subject
//                if (dt != DTERNAL && dt != 0 && derived.isCommutative()) {
//
//                    Term bt = p.beliefTerm;
//                    Term d0 = derived.sub(0);
//
//                    if (derivationMatch(bt, d0))
//                        dt *= -1;
//                }
//                if (derived.op().temporal) {
//                    occReturn[1] = union.b; //extend <=> and ==>
//                }
//
//                return deriveDT(derived, polarity, dt, occReturn, p);
//
//
//            }
//
//
//        }
//
//        //conjunction duration??
////        if (beliefStart != ETERNAL) {
////            occReturn[0] = beliefStart;
////            occReturn[1] = p.belief.end();
////        } else if (taskStart != ETERNAL) {
////            occReturn[0] = taskStart;
////            occReturn[1] = p.task.end();
////        }
//
//        return derived;
//    }
//
//    @Nullable
//    static Term merge(@NotNull Term a, long aStart, @NotNull Term b, long bStart) {
//
//        List<ObjectLongPair<Term>> events = $.newArrayList();
//
//        a.events(events, aStart);
//        b.events(events, bStart);
//
//        events.sort((x, y) -> Long.compare(x.getTwo(), y.getTwo()));
//
//
//        int ee = events.size();
//        assert (ee > 1);
//
//        //group all parallel clusters
//        {
//            Term head = events.get(0).getOne();
//            long headAt = events.get(0).getTwo();
//            int groupStart = -1;
//            for (int i = 1; i <= ee; i++) {
//                long nextAt = (i != ee) ? events.get(i).getTwo() : ETERNAL;
//                if (nextAt == headAt) {
//                    if (groupStart == -1) groupStart = i - 1;
//                } else {
//                    if (groupStart != -1) {
//                        int groupEnd = i;
//                        Term[] p = new Term[groupEnd - groupStart];
//                        assert (p.length > 1);
//                        long when = events.get(groupStart).getTwo();
//                        for (int k = 0, j = groupStart; j < groupEnd; j++) {
//                            p[k++] = events.get(groupStart).getOne();
//                            events.remove(groupStart);
//                            i--;
//                            ee--;
//                        }
//                        Term replacement = $.parallel(p);
//                        if (replacement == null)
//                            return null; //failure
//                        if (events.isEmpty()) {
//                            //got them all here
//                            return replacement;
//                        }
//                        events.add(i, PrimitiveTuples.pair(replacement, when));
//                        i++;
//                        ee++;
//                        groupStart = -1; //reset
//                    }
//                }
//                headAt = nextAt;
//            }
//        }
//
//        {
//            if (ee == 1) {
//                return compoundOrNull(events.get(0).getOne());
//            } else if (ee == 0) {
//                return null;
//            }
//
//            Term head = events.get(0).getOne();
//            long headAt = events.get(0).getTwo();
//            for (int i = 1; i < ee; i++) {
//
//                Term next = events.get(i).getOne();
//                long nextAt = events.get(i).getTwo();
//
//                int dt = (int) (nextAt - headAt);
//                head = $.seq(head, dt, next);
//                if (head == null)
//                    return null;
//
//                headAt = nextAt;
//            }
//            return head;
//        }
//
//    }
//
//    @Deprecated
//    static boolean derivationMatch(@NotNull Term a, @NotNull Term b) {
//        return /*productNormalize*/(a.unneg()).equalsIgnoringVariables(/*productNormalize*/b, true);
//    }
//
////    static boolean derivationMatch(@NotNull Term a, @NotNull Term b, @NotNull Derivation p) {
////        Term pa = resolve(p, a);
////        if (pa!=null) {
////            Term pb = resolve(p, b);
////            if (pb!=null) {
////                return pa.unneg().equalsIgnoringVariables(pb);
////            }
////        }
////        return false;
////    }
//
//
//    /**
//     * copiesthe 'dt' and the occurence of the task term directly
//     */
//    TimeFunctions dtTaskExact = (Term derived, @NotNull Derivation p, long[] occReturn, float[] confScale) ->
//            dtExact(derived, occReturn, p, true, +1);
//    TimeFunctions dtBeliefExact = (Term derived, @NotNull Derivation p, long[] occReturn, float[] confScale) ->
//            dtExact(derived, occReturn, p, false, +1);
////    TimeFunctions dtBeliefReverse = (Term derived, @NotNull Derivation p, long[] occReturn, float[] confScale) ->
////            dtExact(derived, occReturn, p, false, -1);
//
//
//    /**
//     * special handling for dealing with detaching, esp. conjunctions which involve a potential mix of eternal and non-eternal premise components
//     */
//    @Nullable TimeFunctions decomposeTask = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) ->
//            decompose(derived, p, occReturn, true);
//    @Nullable TimeFunctions decomposeBelief = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) ->
//            decompose(derived, p, occReturn, false);
//
//    @Nullable TimeFunctions decomposeBeliefLate = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) ->
//            decomposeLate(derived, p, occReturn, false);
//
//    static Term decomposeLate(@NotNull Term derived, @NotNull Derivation p, @NotNull long[] occReturn, boolean b) {
//        return lateIfGoal(p, occReturn, decompose(derived, p, occReturn, b));
//    }
//
//    ;
//
//    static Term lateIfGoal(@NotNull Derivation p, @NotNull long[] occReturn, @Nullable Term x) {
//        if ((x != null) && p.task.isGoal() && p.concPunc == GOAL && (occReturn[0] != ETERNAL)) {
//            long taskStart = p.task.start();
//
//
//            //dont derive a past-tense goal (before the task)
//            if (taskStart != ETERNAL) {
//                long now = p.nar.time();
//
//                //occReturn[1] = occReturn[0]; //HACK
//
//                long derivedStart = occReturn[0];
//                if (taskStart > derivedStart) {
//
//                    long range = Math.max(occReturn[1] - occReturn[0], p.task.dtRange());
//
//                    occReturn[0] = taskStart;
//                    occReturn[1] = taskStart + range;
//                } /*else if (taskStart < now) {
//                    long range = occReturn[1] - occReturn[0];
//
//                    taskStart = now; //imminanentize the eschaton
//                    occReturn[0] = now;
//                    occReturn[1] = now+ range;
//                }*/
//
//            }
//
//        }
//        return x;
//    }
//
//
//    /**
//     * the 2-ary result will have its 'dt' assigned by the occurrence of its subterms in the task's compound
//     */
//    @Nullable TimeFunctions decomposeTaskComponents = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) -> {
//
//        if (derived.size() == 2) {
//
//            Term from = p.task.term();
//            int occA = from.subtermTime(derived.sub(0));
//
//            if (occA != DTERNAL) {
//
//                int occB = from.subtermTime(derived.sub(1));
//
//                if (occB != DTERNAL) {
//                    if (!p.task.isEternal()) {
//                        occReturn[0] = p.task.start() + Math.min(occA, occB);
//                    }
//                    int dt = occB - occA;
//                    return deriveDT(derived, +1, dt, occReturn, p);
//                }
//            }
//        }
//
//        return null;
//    };
//
//    /*** special case for decomposing conjunctions in the task slot.
//     *   has special case for decomposing goal conjunctions (earliest component only */
//    @Nullable TimeFunctions decomposeTaskSubset = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, float[] confScale) -> {
//        Task task = p.task;
//        Term taskTerm = task.term();
//
//        int taskSize = taskTerm.size();
//
//        if (derived.op() == CONJ && (task.volume() == derived.volume() && taskSize == derived.size() && task.term().vars() == derived.vars())) {
//            //something wrong happened with the ellipsis selection.
//            //being a decomposition it should produce a smaller result
//            throw new RuntimeException(derived + " " + "ellipsis commutive match fault: same as parent");
//        }
//
//        Task belief = p.belief;
//        if (taskSize <= 2) { //conjunction of 1 can not occur actually, but for completeness use the <=
//            //a decomposition of this will result in a complete subterm (half) of the input.
//            //if (dt!=DTERNAL || dt!=XTERNAL || dt!=0) {
//            //call "decomposeTask"
//            //return decompose(derived, p, occReturn, true);
//            Term resolvedDerived = resolve(derived, p);
//
//            Term resolvedTaskTerm = resolve(taskTerm, p);
//            if (resolvedTaskTerm != null) {
//                int derivedInTask = resolvedTaskTerm.subtermTime(resolvedDerived);
//
//                if (derivedInTask != DTERNAL) {
//
////                    if (task.isGoal() && derivedInTask != 0) {
////                        //if this is the result of the structural decompose: only decompose the earliest component of a conjunction goal
////                        //TODO this could be tested sooner in the derivation, here it has already been nearly constructed just to fail
////                        //otherwise it is the conditional decompose
////                        //                    if (d.goal.single())
////                        //                        return null;
////                    }
//
//                    if (!task.isEternal()) {
//                        occReturn[0] = task.start() + derivedInTask;
//                        occReturn[1] = occReturn[0] + (derived.op() == CONJ ? derived.dtRange() : 0);
//                    } else if (belief != null && !belief.isEternal()) {
//                        int timeOfBeliefInTask = resolvedTaskTerm.subtermTime(resolve(p.beliefTerm, p));
//                        if (timeOfBeliefInTask == DTERNAL)
//                            timeOfBeliefInTask = 0;
//                        long taskOcc = belief.start() - timeOfBeliefInTask;
//                        occReturn[0] = taskOcc + derivedInTask;
//                        occReturn[1] = occReturn[0] + (derived.op() == CONJ ? derived.dtRange() : 0);
//                    } else {
//                        //both eternal - no temporal basis
//                        return null;
//                    }
//                } else {
//                    //TODO offset dt could not be determined; this may occurr in tasks/derivations which consist entirely of variables
//                    return null;
//                }
//            }
//
//        } else {
//
//            //3 or more
//            //if (derived.op() == CONJ) {
//            int dt = task.dt(); //copy either the DTERNAL or 0 commutive timing
//
//            if (!task.isEternal()) {
//                occReturn[0] = task.start();
//            } else if ((belief != null && !belief.isEternal())) {
//                Term resolvedTaskTerm = resolve(taskTerm, p);
//                int timeOfBeliefInTask = resolvedTaskTerm.subtermTime(p.beliefTerm);
//                occReturn[0] = belief.start() - timeOfBeliefInTask;
//            } else {
//                return null;
//            }
//
//            occReturn[1] = occReturn[0] + derived.dtRange();
//
//            derived = deriveDT(derived, +0, dt, occReturn, p);
//            //} else {
//            //    //TODO this should not happen
//            //    return null;
//            //}
//        }
//
//        //return derived;
//        return lateIfGoal(p, occReturn, derived);
//    };
//
//    @Nullable
//    static Term decompose(@NotNull Term derived, @NotNull Derivation p, @NotNull long[] occReturn, boolean decomposeTask) {
//
//
//        Task task = p.task;
//        Task belief = p.belief;
//
//        Task otherTask = (decomposeTask) ? belief : task;
//
//        Term decomposedTerm = (decomposeTask ? p.taskTerm : p.beliefTerm);
//
//        //occOther: the non-decomposed counterpart of the premise
//
////        long occDecomposed = task.start();
////        long occOther = belief!=null ? belief.start() : ETERNAL;
////        if (task.start() == ETERNAL && belief!=null) {
////            occDecomposed = belief.start();
////            occOther = task.start();
////        }
//
//        long occDecomposed = decomposeTask ? task.start() : (belief != null ? belief.start() : task.start());
//        long occOther = (otherTask != null) ? otherTask.start() : ETERNAL;
//        Term otherTerm = decomposeTask ? p.beliefTerm : p.taskTerm;
//
//        if (occDecomposed == ETERNAL && occOther != ETERNAL)
//            occDecomposed = occOther; //use the only specified time
//
//        if ((occDecomposed == ETERNAL) && (occOther == ETERNAL)) {
//            //no temporal basis that can apply. only derive an eternal result
//            // if there is no actual temporal relation in the decomposition
//
//            int ddt = decomposedTerm.dt();
//            if ((ddt == 0 || ddt == DTERNAL || ddt == XTERNAL)) {
//                return derived; //allow eternal decompose since no temporal information is inside the decomposed term
//            } else {
//                return derived;
//                //noTemporalBasis(derived);
//            }
//
//        }
//
//        long occ = ETERNAL;
//
//        if (decomposeTask) {
//
//            if (occDecomposed != ETERNAL) {
//
//                if (occOther!=ETERNAL) {
//                    //choose randomly between the two occurrence times
//                    Task which = chooseByConf(task, belief, p);
//                    if (which == otherTask) {
//                        occDecomposed = occOther;
//                    }
//                }
//
//                Compound rDecomposedTerm = compoundOrNull(resolve(decomposedTerm, p));
//                if (rDecomposedTerm != null) {
//                    Term rDerived = resolve(derived, p);
//                    if (rDerived != null) {
//                        occ = solveDecompose(occDecomposed, rDerived, rDecomposedTerm); //occOther is the belief's start time
//                    }
//                }
//
//            }
//        } else {
//
//            //occOther is the task's start time. try this first
//            Term rDerived = resolve(derived, p);
//            if (rDerived != null) {
//
//                final Term rDecomposed = resolve(decomposedTerm, p);
//                if (rDecomposed instanceof Compound) {
//
//                    if (occOther != ETERNAL) {
//                        Term rOtherTerm = resolve(otherTerm, p);
//
//                        long derivedInTask = solveDecompose(occOther, rDerived, (Compound)rDecomposed);
//
//                        if (derivedInTask != ETERNAL) {
//
//                            int taskInBelief = rDecomposed.subtermTime(rOtherTerm);
//                            if (taskInBelief != DTERNAL) {
//                                occ = derivedInTask - taskInBelief - derived.dtRange();
//                            }
//
//                        } else {
//                            if (rDerived != null && rOtherTerm != null) {
//                                int derivedInBelief = rDecomposed.subtermTime(rDerived);
//                                if (derivedInBelief != DTERNAL) {
//                                    int taskInBelief = rDecomposed.subtermTime(rOtherTerm);
//                                    if (taskInBelief != DTERNAL) {
//                                        occ = occOther /* task occ */ + derivedInBelief - taskInBelief - derived.dtRange();
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    if (occ == ETERNAL && occDecomposed != ETERNAL) {
//                        //task could not resolve temporally; try to resolve using only the belief
//                        occ = solveDecompose(occDecomposed, rDerived, (Compound)rDecomposed);
//                    }
//
//
//                }
//
////                if (derivedInTask == ETERNAL) {
//                //try further:
////                    Compound rDecomposedTerm = compoundOrNull(resolve(p, decomposedTerm));
////                    if (rDecomposedTerm != null) {
////                            if (rDerived != null) {
////                                long taskInBelief = solveDecompose(occOther, rOtherTerm, p, rDecomposedTerm);
////
////                                long derivedInBelief = solveDecompose(
////                                        //long derivedInTask
////                            }
////                        }
////                    }
////                }
//
//            }
//        }
//
////        if (occ == ETERNAL && occOther != ETERNAL) {
////            //try to resolve by using the other's occ
////            if (occ == ETERNAL && occOther != ETERNAL) {
////
////
////                @Nullable Term rOtherTerm = resolve(p, decomposeTask ? p.beliefTerm : p.taskTerm);
////                if (rOtherTerm != null) {
////
////                    @Nullable Term rDecomposed = resolve(p, decomposedTerm);
////
////                    int otherInDecomposed = rDecomposed.subtermTime(rOtherTerm);
////
////                    @Nullable Term rDerived = resolve(p, derived);
////
////                    if (decomposedTerm.dt() == 0 && otherInDecomposed == 0) {
////                        //special case for &&+0 having undergone some unrecognizable change
////                        occ = occOther - 0; //+0 should ensure it has the same time as this sibling event
////
////                    } else if (rDerived != null) { //{ && otherInDecomposed != DTERNAL) {
////                        int derivedInDecomposed = rDecomposed.subtermTime(rDerived);
////                        if (derivedInDecomposed != DTERNAL) {
////                            occ = occOther + derivedInDecomposed;
////                            if (otherInDecomposed != DTERNAL) { //???
////                                if (derivedInDecomposed == 0) {
////                                    occ -= otherInDecomposed + rDerived.dtRange();
////                                } else {
////                                    occ -= otherInDecomposed; //<---- TODO subsent_1
////                                }
////                            }
////
////                        } else {
////                            //could not back substitute resolve
////                            return null;
////                        }
////                    }
////
////                }
////
////            }
////
////        }
//
////        if (decomposedTerm.size() != 2) {
////            //probably a (&&+0, ...)
////            occ = occDecomposed != ETERNAL ? occDecomposed : occOther;
////        } else
////         {
////
////            @Nullable Term rDecomposed = resolve(p, decomposedTerm);
////
////            if (rDecomposed!=null) {
////
////                @Nullable Term rDerived = resolve(p, derived);
////
////                if (rDerived != null) {
////                    int dt = rDecomposed.subtermTime(rDerived);
////
////                    //prefer the task's occurrence first
////                    if (dt != DTERNAL) {
////                        if (occDecomposed != ETERNAL) {
////                            occ = occDecomposed + dt;
////                        } else {
////
////                            //try resolving in the otherTask ?
////
//////                        if (!task.isEternal()) {
////
//////                            occ = task.start() + dt;
//////
//////                            Term rTaskTerm = resolve(p, task.term());
//////                            if (rTaskTerm!=null) {
//////                                Term rOtherTerm = resolve(p, p.beliefTerm);
//////                                if (rOtherTerm != null) {
//////                                    int taskInDecomposed = rDecomposed.subtermTime(rTaskTerm);
//////                                    if (taskInDecomposed!= DTERNAL) { //???
//////                                        int otherInDecomposed = rDecomposed.subtermTime(rOtherTerm);
//////                                        if (otherInDecomposed != DTERNAL) { //???
//////                                            occ = task.start() - (otherInDecomposed + rDerived.dtRange()) - taskInDecomposed + dt;
//////                                        }
//////                                    }
//////                                }
//////                            }
//////
//////                        } else if (occDecomposed != ETERNAL) {
//////
//////                            occ = occDecomposed + dt;
//////
//////                        }
//////
////                        }
////                    }
////                }
////
////
////                //default:
////                /*if (occ == ETERNAL)
////                    occ = occOther;*/
////
////            }
////        }
//
//
//        if (occ == ETERNAL) {
//            //return noTemporalBasis(derived);
//            return null;
//        }
//
//
//        occReturn[0] = occ;
//
//        //TODO decide if this is right
//        if (occ != ETERNAL)
//            occReturn[1] = occ + derived.dtRange();
//
//
//        return derived;
//
//    }
//
//    static long solveDecompose(long occ, @NotNull Term x, @Nullable Compound container) {
//
//        assert (occ != ETERNAL);
//
//        if (container != null) {
//            int dt = container.subtermTime(x);
//            if (dt != DTERNAL) {
//                return occ + dt;
//            }
//        }
//
//        return ETERNAL;
//    }
//
//    @NotNull
//    static Term resolve(@NotNull Term x, @NotNull Derivation p) {
//        Term y;
//        try {
//            y = p.transform(x);
//        } catch (InvalidTermException e) {
//            //failed, just return the input
//            y = x;
//        }
//
//        return y != null ? y : x;
//
//    }
//
////    @Nullable
////    static Compound noTemporalBasis(@NotNull Compound derived) {
//////        if (Param.DEBUG_EXTRA)
//////            logger.info("{}", new InvalidTermException(derived.op(), derived.dt(), "no basis for relating other occurrence to derived", derived.toArray());
////
////
////        return null;
////    }
//
//
//    @Nullable
//    static Term dtExact(@NotNull Term derived, @NotNull long[] occReturn, @NotNull Derivation p, boolean taskOrBelief, int polarity) {
//
//        Term dtTerm = taskOrBelief ? p.taskTerm.unneg() : p.beliefTerm;
//
//        Task t = p.task;
//        Task b = p.belief;
//
//        long tOcc = t.start();
//        if (b != null) {
//            if (t.isQuestOrQuestion() || tOcc == ETERNAL)
//                tOcc = b.start(); //use belief time when task is a question, or task is eternal
//        }
//
//        Term dtTermResolved = resolve(dtTerm, p);
//        Term derivedResolved = resolve(derived, p);
//        if (!taskOrBelief && b != null && dtTermResolved != null && derivedResolved != null) {
//            //if (b.occurrence()!=ETERNAL) {
//            int derivedInT = dtTermResolved.subtermTime(derivedResolved);
//            if (derivedInT == DTERNAL && derivedResolved.op() == Op.IMPL) {
//                //try to find the subtermTime of the implication's subject
//                derivedInT = dtTermResolved.subtermTime((derivedResolved).sub(0));
//            }
//
//            if (derivedInT == DTERNAL) {
//                derivedInT = 0;
//            }
//
//            if (tOcc != ETERNAL)
//                occReturn[0] = tOcc + derivedInT;
//
////            } else if (t.occurrence()!=ETERNAL) {
////                //find the offset of the task term within the belief term, and then add the task term's occurrence
////                occReturn[0] =
////            }
//        } else {
//            occReturn[0] = tOcc; //the original behavior, but may not be right
//        }
//
//
//        int dtdt =  dtTerm.dt();
//        return deriveDT(derived, polarity, dtdt, occReturn, p);
//
//    }
//
//    @Nullable
//    static Term deriveDT(@NotNull Term derived, int polarity, int eventDelta, @NotNull long[] occReturn, Derivation p) {
//
//        int dt = eventDelta == DTERNAL ? DTERNAL : eventDelta * polarity;
//
//        return dt(derived, dt, occReturn, p);
//    }
//
//
//    @NotNull TimeFunctions dtCombine = (Term derived, @NotNull Derivation p, long[] occReturn, float[] confScale) ->
//            dtCombiner(derived, p, occReturn, false, false);
//
//    @NotNull TimeFunctions dtCombinePre = (Term derived, @NotNull Derivation p, long[] occReturn, float[] confScale) ->
//            dtCombiner(derived, p, occReturn, true, false);
//
//    @NotNull TimeFunctions dtCombinePost = (Term derived, @NotNull Derivation p, long[] occReturn, float[] confScale) ->
//            dtCombiner(derived, p, occReturn, false, true);
//
//
//    /**
//     * combine any existant DT's in the premise (assumes both task and belief are present)
//     */
//    @Nullable
//    static Term dtCombiner(@NotNull Term derived, @NotNull Derivation p, long[] occReturn, boolean pre, boolean post) {
//
//        Task task = p.task;
//        int taskDT = ( p.taskTerm.unneg()).dt();
//
//        Term bt = p.beliefTerm;
//        int beliefDT = (bt instanceof Compound) ? ( bt.unneg()).dt() : DTERNAL;
//        int eventDelta = DTERNAL;
//
//        if (derived.size() > 1) {
//
//
//            if (taskDT == DTERNAL && beliefDT == DTERNAL) {
//
//                //eventDelta = DTERNAL;
//
//            } else if (taskDT != DTERNAL && beliefDT != DTERNAL) {
//
//                if (derived.size() != 2)
//                    throw new RuntimeException(derived +  " expectd arity=2");
//
//                //assume derived has 2 terms exactly
//                Term da = derived.sub(0);
//                Term db = derived.sub(1);
//
//                Term tt = task.term();
//                int ta = tt.subtermTime(da);
//                int tb = tt.subtermTime(db);
//
//                Term btc = bt;
//                int ba = btc.subtermTime(da);
//                int bb = btc.subtermTime(db);
//
//                int dtT, dtB;
//                if (ta != DTERNAL && tb != DTERNAL && ba != DTERNAL && bb != DTERNAL) {
//                    //compare between subterms both present in the premise
//                    dtT = tb - ta;
//                    dtB = bb - ba;
//                } else {
//                    //compare between one set of common subterms, the other is a new introduced term
//                    if (ta == DTERNAL && ba == DTERNAL) {
//                        dtT = taskDT;
//                        dtB = beliefDT;
//                    } else if (tb == DTERNAL && bb == DTERNAL) {
//                        dtT = taskDT;
//                        dtB = beliefDT;
//                    } else {
//                        return null;
//                    }
//                }
//
//
//                Task chosen = task.isBeliefOrGoal() ? chooseByConf(task, p.belief, p) : p.belief;
//                if (chosen == task)
//                    eventDelta = dtT;
//                else
//                    eventDelta = dtB;
//
//
//            } else if (taskDT == DTERNAL) {
//                eventDelta = beliefDT;
//            } else /*if (beliefDT == DTERNAL)*/ {
//                eventDelta = taskDT;
//            }
//        }
//
//        occReturn[0] = occurrenceTarget(p, earliestOccurrence);
//
//        if (pre && derived.sub(0).unneg() instanceof Compound) {
//            //set subterm 0's DT
//
//
//            Term preSub =  derived.sub(0);
//            boolean neg = preSub.op() == NEG;
//            if (neg) {
//                preSub = preSub.unneg(); //unwrap
//            }
//
//            int preDT;
//            if (preSub.size()==2 && ((taskDT != DTERNAL) && (taskDT != XTERNAL) && (beliefDT != DTERNAL) && (beliefDT != XTERNAL))) {
//                preDT = (taskDT - beliefDT);
//                if (!task.term(0).equals(preSub.sub(0))) {
//                    preDT = -preDT; //reverse the order
//                    eventDelta -= Math.abs(preDT);
//                } else {
//                    eventDelta += Math.abs(preDT);
//                }
//            } else {
//                preDT = 0; //DTERNAL; //??
//            }
//
//            Term newPresub = p.terms.the((Compound)preSub, preDT);
//
//            derived = (p.terms.the((Compound)derived,
//                    new Term[]{$.negIf(newPresub, neg), derived.sub(1)})
//            );
//        }
//        if (post && derived.sub(1) instanceof Compound && derived.sub(1).size() == 2) {
//
//            Term postSub =  derived.sub(1);
//            boolean neg = postSub.op() == NEG;
//            if (neg) {
//                postSub = postSub.unneg(); //unwrap
//            }
//
//            int postDT;
//            if ((taskDT != DTERNAL) && (taskDT != XTERNAL) && (beliefDT != DTERNAL) && (beliefDT != XTERNAL)) {
//                postDT = (taskDT - beliefDT);
//                eventDelta -= postDT;
//
//                if (task.term().size() > 1 && postSub.size() > 1 && !task.term(1).equals(postSub.sub(1))) {
//                    postDT = -postDT; //reverse the order
//                }
//
//            } else {
//                postDT = DTERNAL;
//            }
//
//
//            //set subterm 1's DT
//            Term newPostSub = p.terms.the((Compound) derived.sub(1), postDT);
//
//            derived =
//                    p.terms.the((Compound)derived, new Term[]{derived.sub(0), $.negIf(newPostSub, neg)});
//
//        }
//
//        if (derived == null)
//            return null;
//
//        Term result = deriveDT(derived, 1, eventDelta, occReturn, p);
//
//        return result;
//    }
//
//
//    /**
//     * "automatic" implementation of Temporalize, used by default. slow and wrong about 25..30% of the time sux needs rewritten or replaced
//     * apply temporal characteristics to a newly derived term according to the premise's
//     */
//    @NotNull TimeFunctions Auto = (derived, p, occReturn, confScale) -> {
//
//        //@NotNull PremiseRule rule = d.rule;
//        //Term tp = rule.getTask();
//        //Term bp = rule.getBelief();
//
//        Task t = p.task;
//        Task b = p.belief;
//
//        boolean te = t.isEternal();
//        boolean be = b != null ? b.isEternal() : true;
//        if (te && !be) {
//            long ba = b.start();
//            long bz = b.end();
//            occReturn[0] = ba;
//            occReturn[1] = bz;
//        } else {
//            long ta = t.start();
//            long tz = t.end();
//            if (!te && be) {
//                occReturn[0] = ta;
//                occReturn[1] = tz;
//            } else if (!te && !be) {
//
//                //TODO cache this in Derivation
//
//                long ba = b.start();
//                long bz = b.end();
//                Interval intersection = Interval.intersect(ta, tz, ba, bz);
//                if (intersection != null) {
//                    occReturn[0] = intersection.a;
//                    occReturn[1] = intersection.b;
//                } else {
//                    //not overlapping at all, compute point interpolation
//                    long dist = Interval.unionLength(ta, tz, ba, bz) - (tz - ta) - (bz - ba);
//                    if (Param.TEMPORAL_TOLERANCE_FOR_NON_ADJACENT_EVENT_DERIVATIONS >= ((float)dist)/p.dur) {
//                        occReturn[0] = occInterpolate(t, b);
//                        return derived;
//                    }
//                    return null;
//                }
//            } else {
//                occReturn[0] = occReturn[1] = ETERNAL;
//            }
//        }
//        return derived;
//
////        long occ = occInterpolate(t, b); //reset
////
////        Compound tt = (Compound) p.taskTerm.unneg();
////        Term bb = p.beliefTerm; // belief() != null ? belief().term() : null;
////
////        int td = tt.dt();
////        int bd = bb instanceof Compound ? ((Compound) bb).dt() : DTERNAL;
////
////        int t = DTERNAL;
////
////        Term cp = d.conclusionPattern; //TODO this may be a wrapped immediatefunction?
////
////        if (derived.op().temporal && cp instanceof Compound) {
////
////            Compound ccc = (Compound) cp;
////            Term ca = ccc.sub(0);
////
////            //System.out.println(tt + " "  + bb);
////
////        /* CASES:
////            conclusion pattern size 1
////                equal to task subterm
////                equal to belief subterm
////                unique term
////            conclusion pattern size 2 (a, b)
////                a equal to task subterm
////                b equal to task subterm
////                a equal to belief subterm
////                b equal to belief subterm
////
////         */
////            int s = cp.size();
////            if (s == 2) {
////                Term cb = ccc.sub(1);
////
////                //chained relations
//////                if (td != DTERNAL && bd != DTERNAL && (tp.size() == 2) && (bp.size() == 2)) {
//////
//////                    Compound tpp = (Compound) tp;
//////                    Compound bpp = (Compound) bp;
//////
//////                    if (derivationMatch(tpp.term(1), bpp.term(0))) {
//////                        t = td + bd;
//////
//////                        //chained inner
//////                        if (!derivationMatch(cb, bpp.term(1))) {
//////                            t = -t; //invert direction
//////                        }
//////                    } else if (derivationMatch(tpp.term(0), bpp.term(1))) {
//////                        //chain outer
//////                        t = td + bd; //?? CHECK
//////                    } else if (derivationMatch(tpp.term(0), bpp.term(0))) {
//////                        //common left
//////                        t = td - bd;
//////                    } else if (derivationMatch(tpp.term(1), bpp.term(1))) {
//////                        //common right
//////                        t = bd - td;
//////                    } else {
//////                        //throw new RuntimeException("unhandled case");
//////                        t = (bd + td) / 2; //???
//////                    }
//////
//////                }
////
////                long to = t.start();
////                long bo = b != null ? b.start() : to;
////
////                int occDiff = (to != ETERNAL && bo != ETERNAL) ? (int) (bo - to) : 0;
////
////                if (td == DTERNAL && bd == DTERNAL) {
////
//////                    int aTask = tp.subtermTime(ca, DTERNAL);
//////                    int aBelief = bp.subtermTime(ca, DTERNAL);
//////                    int bTask = tp.subtermTime(cb, DTERNAL);
//////                    int bBelief = bp.subtermTime(cb, DTERNAL);
//////
//////                    if (belief != null) {
//////
//////                        boolean reversed = false;
//////                    /* reverse subterms if commutive and the terms are opposite the corresponding pattern */
//////                        if (derived.op().commutative) {
//////                            if (!derivationMatch(
//////                                    p.resolve(((Compound) cp).term(0)),
//////                                    derived.term(0))) {
//////                                occDiff = -occDiff;
//////                                reversed = true;
//////                            }
//////                        }
//////
//////
//////                        if (aTask != DTERNAL && aBelief == DTERNAL &&
//////                                bBelief != DTERNAL && bTask == DTERNAL) {
//////                            //forward: task -> belief
//////                            //t = (int) (task.occurrence() - belief().occurrence());
//////                            t = occDiff;
//////                            if (reversed) occ -= t;
//////                            else occ += t;
//////
//////                        } else if (aTask == DTERNAL && aBelief != DTERNAL &&
//////                                bBelief == DTERNAL && bTask != DTERNAL) {
//////                            //reverse: belief -> task
//////                            t = -occDiff;
//////                            //t = (int) (belief().occurrence() - task.occurrence());
//////                            //t = (int) (task.occurrence() - belief().occurrence());
//////
//////                            if (!reversed) {
//////                                occ -= t;
//////                            } else {
//////                                occ += t;
//////                            }
//////
//////
//////                        } else {
//////
//////                            //both ITERNAL
//////
//////                            if ((to != ETERNAL) && (bo != ETERNAL)) {
//////                                t = occDiff;
//////                                if (reversed) occ -= t;
//////                                else occ += t;
//////                            }
//////
//////                        }
//////                    }
////
////                } else if (td == DTERNAL) {
////                    //belief has dt
////                    t = bd;// + occDiff;
////                    //TODO align
////                } else if (bd == DTERNAL) {
////                    //task has dt
////                    t = td + occDiff;
////                    //occ += t; //TODO check this alignment
////
////                } else {
////                    //t = occDiff;
////                    //throw new RuntimeException("unhandled case");
////                    //???
////                    //t = (td+bd)/2;
////                }
////            }
////
////        }
////
////
////        //apply occurrence shift
////        if (occ != ETERNAL) {
////
////            Term T = resolve(tt, p);
////            if (T != null) {
////                Term B = resolve(bb, p);
////
////                if (b != null) {
////                    //TODO cleanup simplify this is messy and confusing
////
////                    if (t.isEternal() && !b.isEternal()) {
////                        //find relative time of belief in the task, relative time of the conclusion, and subtract
////                        //the occ (=belief time's)
////                        long timeOfBeliefInTask = T.subtermTime(B) + td;
////                        long timeOfDerivedInTask = T.subtermTime(derived) + td;
////                        if (timeOfDerivedInTask != DTERNAL && timeOfBeliefInTask != DTERNAL)
////                            occ += (timeOfDerivedInTask - timeOfBeliefInTask);
////                        else if (timeOfDerivedInTask != DTERNAL)
////                            occ += timeOfDerivedInTask;
////                    } else if (!t.isEternal() && b.isEternal() && B != null) {
////                        long timeOfTaskInBelief = B.subtermTime(T) + bd;
////                        long timeOfDerivedInBelief = B.subtermTime(derived) + bd;
////
////                        if (timeOfTaskInBelief != DTERNAL && timeOfDerivedInBelief != DTERNAL)
////                            occ += (timeOfDerivedInBelief - timeOfTaskInBelief);
////                        else if (timeOfDerivedInBelief != DTERNAL)
////                            occ += timeOfDerivedInBelief;
////                        else {
////                            long timeOfDerivedInTask = T.subtermTime(derived) + td;
////                            if (timeOfDerivedInTask != DTERNAL) {
////                                occ += timeOfDerivedInTask;
////                            } else {
////                                //??
////                            }
////                        }
////                    } else if (!t.isEternal() && !b.isEternal()) {
////                        //throw new RuntimeException("ambiguous task or belief");
////
////                        //long ot = T.subtermTime(C, td);
////                        //long ob = B.subtermTime(C, bd);
////                        //if (t!=ITERNAL)
////                        //    occ -= t;
////                    }
////                } else {
////
////                    if (!t.isEternal()) {
////                        long timeOfDerivedInTask = T.subtermTime(derived) + td;
////                        if (timeOfDerivedInTask != DTERNAL)
////                            occ += timeOfDerivedInTask;
////                    } else {
////
//////                        int ot = tp.subtermTime(cp, td);
//////                        int ob = bp.subtermTime(cp, bd);
//////
//////                        if (ot != DTERNAL) {
//////                            if (tp instanceof Compound) {
//////                                Compound ctp = (Compound) tp;
//////                                if (derivationMatch(ctp.term(0), cp)) {
//////                                    ot -= td;
//////                                }
//////                            }
//////                            occ += ot; //occ + ot;
//////                        } else if (ob != DTERNAL) {
//////
//////                            if (belief.start() != task.start()) { //why?
//////                                if (bp instanceof Compound) {
//////                                    Compound cbp = (Compound) bp;
//////                                    if (!derivationMatch(cbp.term(1), cp)) {
//////                                        ob -= bd;
//////                                    }
//////                                }
//////                            }
//////
//////                            occ += ob;
//////
//////                        } else {
//////                            //neither, remain eternal
//////                            throw new RuntimeException("unhandled case");
//////                        }
////                    }
////                }
////            }
////
////
////        }
////        //}
////        //}
////
////
////        if ((t != DTERNAL) && (t != derived.dt())) {
////        /*derived = (Compound) p.premise.nar.memory.index.newTerm(derived.op(), derived.relation(),
////                t, derived.subterms());*/
////
////            if (derived.size() == 2 || t == 0)
////                derived = dt(derived, t, occReturn, p);
////
//////            int nt = derived.t();
//////            if (occ > TIMELESS) {
//////                if (Math.signum(t) != Math.signum(nt)) {
//////                    //re-align the occurrence
//////                    occ -= t;
//////                } else {
//////                    occ -= nt;
//////                }
//////            }
////        }
////
//////        if (belief!=null) {
//////            long taskOcc = task.start();
//////            if (taskOcc!=ETERNAL) {
//////                long belOcc = belief.start();
//////                if (belOcc!=ETERNAL) {
//////                    Interval ii = Interval.union(taskOcc, task.end(), belOcc, belief.end() );
//////                    if (ii != null) {
//////                        occReturn[0] = ii.a;
//////                        occReturn[1] = ii.b;
//////
//////                        return derived;
//////                    } else {
//////                        //no intersection: point-like below
//////                    }
//////                }
//////            }
//////        } else {
//////            //inherit task's occurrence exactly
//////            occReturn[0] = task.start();
//////            occReturn[1] = task.end();
//////            return derived;
//////        }
////
////        occReturn[0] = occ;
//
//    };
//
//    static long occInterpolate(@NotNull Task t, @Nullable Task b) {
//
//        long to = t.mid();
//        if (b == null) {
//            return to;
//        }
//
//        long bo = b.mid();
//        if (t.isQuestOrQuestion() || to == ETERNAL)
//            return bo; //use belief time when task is a question, or task is eternal
//
//        if (bo == ETERNAL)
//            return to; //dont uneternalize belief, defer to task's occurrence
//
//        //if (to != ETERNAL && bo != ETERNAL) {
//
//        @Nullable Interval intersection = Interval.intersect(t.start(), t.end(), b.start(), b.end());
//        if (intersection != null) {
//            return intersection.mid(); //?
//        }
//
//        float tw = t.evi();
//        float bw = b.evi();
//        return Util.lerp((tw) / (bw + tw), bo, to);
//
//
////        } else {
////            return bo != ETERNAL ? bo : to;
////        }
//
//    }
//
//
//    static Term dt(@NotNull Term derived, int dt, long[] occReturn, Derivation p) {
//        Op o = derived.op();
//        if (!o.temporal) {
//            dt = DTERNAL;
//        }
////        if (!o.temporal && dt != DTERNAL && dt != 0 && occReturn[0] != ETERNAL) {
////            //something got reduced to a non-temporal, so shift it to the midpoint of what the actual term would have been:
////            occReturn[0] += dt / 2;
////            dt = DTERNAL;
////        }
//        if (derived.dt() != dt) {
//
//            @NotNull Term n = derived.dt(dt); //compoundOrNull(p.terms.the(o, dt, ds));
//            if (n == null)
//                return null; //throw new InvalidTermException(o, dt, ds, "Untemporalizable to new DT");
//
//            //TODO decide if this is correct
//            if (
//                    o.temporal
//                            //o==CONJ //conjunctions only
//                            && occReturn[0] != ETERNAL && occReturn[1] == ETERNAL)
//                occReturn[1] = occReturn[0] + n.dtRange();
//
//            return n;
//        } else {
//            return derived;
//        }
//    }
//
//
//    public static long occurrenceTarget(Derivation p, @NotNull OccurrenceSolver s) {
//        long tOcc = p.task.start();
//        Task b = p.belief;
//        if (b == null) {
//            return tOcc;
//        } else {
//            long bOcc = b.start();
//            return s.compute(tOcc, bOcc);
//
////            //if (bOcc == ETERNAL) {
////            return (tOcc != ETERNAL) ?
////                        whenBothNonEternal.compute(tOcc, bOcc) :
////                        ((bOcc != ETERNAL) ?
////                            bOcc :
////                            ETERNAL
////            );
//        }
//    }
//}
