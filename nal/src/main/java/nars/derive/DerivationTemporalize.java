package nars.derive;

import jcog.Util;
import nars.Param;
import nars.Task;
import nars.control.Derivation;
import nars.derive.time.AbsoluteEvent;
import nars.derive.time.Event;
import nars.derive.time.Temporalize;
import nars.derive.time.Time;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.subst.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static nars.Op.IMPL;
import static nars.time.Tense.ETERNAL;

/**
 * unknowns to solve otherwise the result is impossible:
 * - derived task start time
 * - derived task end time
 * - dt intervals for any XTERNAL appearing in the input term
 * knowns:
 * - for each task and optional belief in the derived premise:
 * - start/end time of the task
 * - start/end time of any contained events
 * - possible relations between events referred to in the conclusion that
 * appear in the premise.  this may be partial due to variable introduction
 * and other reductions. an attempt can be made to back-solve the result.
 * if that fails, a heuristic could decide the match. in the worst case,
 * the derivation will not be temporalizable and this method returns null.
 *
 * @param eviGain length-1 float array. the value will be set to 1f by default
 */
public class DerivationTemporalize extends Temporalize {

    private final Task task, belief;

    public DerivationTemporalize(Derivation d) {
        super(d.random);
        task = d.task;
        belief = d.belief; //!d.single ? d.belief : null;
        dur = Math.max(1, Math.round(d.nar.dtDither.floatValue() * d.dur));


        knowDerivedAbsolute(d,
                polarizedTaskTerm(task),
                task.start(), task.end());

        if (belief != null) {
            if (!belief.equals(task)) {

                knowDerivedAbsolute(d,
                        polarizedTaskTerm(belief),
                        belief.start(), belief.end()); //!taskRooted || !belief.isEternal()); // || (bo != IMPL));
            }
        } else /*if (d.beliefTerm != null)*/ {
            if (!task.term().equals(d.beliefTerm)) { //dont re-know the term

                Term b = d.beliefTerm;
                knowDerivedAmbient(d, b);

            }
            //t.know(d.beliefTerm, d, null);
        }


    }

    /** negate if negated, for precision in discriminating positive/negative */
    static Term polarizedTaskTerm(Task t) {
        return t.term().negIf(t.truth()!=null && t.truth().isNegative());
    }

    void knowDerivedAmbient(Subst d, Term x) {
        knowAmbient(x);
        if (knowTransformed) {
            Term x2 = d.transform(x);
            if (!x2.equals(x) && !(x2 instanceof Bool))
                knowAmbient(x2);
        }
    }

    void knowDerivedAbsolute(Subst d, Term term, long start, long end) {
        if (term.op() == IMPL && (!fullyEternal())) {
            //only know an impl as ambient if there is already non-eternal events detected
            knowDerivedAmbient(d, term);
            return;
        }

        knowAbsolute(term, start, end);

        if (knowTransformed) {
            Term t2 = d.transform(term);
            if (!t2.equals(term) && !(t2 instanceof Bool)) {
                knowAbsolute(t2, start, end);
            }
        }
    }

    @Nullable
    public Term solve(@NotNull Derivation d, Term pattern, long[] occ, float[] eviGain) {

        Task belief = d.single ? null : this.belief; //if single, ignore belief occurrence HACK this may not completely ignore any absolute values associated with events contained in the belief

        Map<Term, Time> trail = new HashMap<>();
        Event e;
        try {
            e = solve(pattern, trail);
        } catch (StackOverflowError ignored) {
            System.err.println(
                    Arrays.toString(new Object[]{"temporalize stack overflow:\n{} {}\n\t{}", pattern, d, trail})
                    //logger.error(
            );
//            trail.clear();
//            model.solve(pattern, trail);
            return null;
        }
        if (e == null || !e.term.op().conceptualizable) {
            return null;
        }

        if (e instanceof AbsoluteEvent) {
            AbsoluteEvent a = (AbsoluteEvent) e; //faster, preferred since pre-calculated
            occ[0] = a.start;
            occ[1] = a.end;
        } else {
            occ[0] = e.start(trail).abs();
            occ[1] = e.end(trail).abs();
        }


        boolean te = task.isEternal();
        if (occ[0] == ETERNAL && (!te || (belief != null && !belief.isEternal()))) {
            //"eternal derived from non-eternal premise:\n" + task + ' ' + belief + " -> " + occ[0];
            //uneternalize/retemporalize:

//            if (/*(e.term.op() != IMPL) && */
//                    (task.op() == IMPL) && (belief == null || d.beliefTerm.op() == IMPL)) {
//                //dont retemporalize a non-implication derived from two implications
//                //it means that the timing is unknown
//                return null;
//            }

            long ts = task.start();
            long k;
            if (!te && (belief != null && !belief.isEternal())) {
                //interpolate
                ts = task.nearestTimeBetween(belief.start(), belief.end());
                long bs = belief.nearestTimeBetween(ts, task.end());
                if (ts != bs) {
                    //confidence decay in proportion to lack of coherence
                    if (task.isBeliefOrGoal()) {
                        float taskEvi = task.evi();
                        float beliefEvi = belief.evi();
                        float taskToBeliefEvi = taskEvi / (taskEvi + beliefEvi);
                        k = Util.lerp(taskToBeliefEvi, bs, ts); //TODO any duration?
                        long distSum =
                                Math.abs(task.nearestTimeTo(k) - k) +
                                        Math.abs(belief.nearestTimeTo(k) - k);
                        if (distSum > 0) {
                            eviGain[0] *= Param.evidenceDecay(1, d.dur, distSum);
                        }
                    } else {
                        k = bs;
                    }
                } else {
                    k = ts;
                }
            } else if (te) {
                k = belief.start(); //TODO any duration?
            } else /*if (be)*/ {
                k = ts; //TODO any duration?
            }
            occ[0] = occ[1] = k;
        }


        return e.term;
    }

}
