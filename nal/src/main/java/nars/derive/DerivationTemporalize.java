package nars.derive;

import jcog.math.Interval;
import nars.$;
import nars.Op;
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

import java.util.*;

import static nars.Op.CONJ;
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

    protected static final boolean knowTransformed = true;

    /**
     * constraints specific for specific double premise derivations
     */
    public Map<Term, SortedSet<Event>> sng, dbl;

    public DerivationTemporalize(Derivation d) {
        super(d.random);
        task = d.task;
        belief = d.belief; //!d.single ? d.belief : null;
        dur = Math.max(1, Math.round(d.nar.dtDither.floatValue() * d.dur));

        knowDerivedAbsolute(d,
                polarizedTaskTerm(task),
                task.start(), task.end());


        if (!task.term().equals(d.beliefTerm)) { //dont re-know the term

            Term b = d.beliefTerm;
            knowDerivedAmbient(d, b);

        }

        sng = constraints;

        if (belief != null && !belief.equals(task)) {

            this.constraints = dbl = new HashMap(); //clone
            sng.forEach((k,v)->{
                dbl.put(k, new TreeSet(v));
            });

            knowDerivedAbsolute(d,
                    polarizedTaskTerm(belief),
                    belief.start(), belief.end()); //!taskRooted || !belief.isEternal()); // || (bo != IMPL));


        } else {
            this.dbl = sng;
        }
    }

    /**
     * negate if negated, for precision in discriminating positive/negative
     */
    static Term polarizedTaskTerm(Task t) {
        return t.term().negIf(t.truth() != null && t.truth().isNegative());
    }

    void knowDerivedAmbient(Subst d, Term x) {
        knowAmbient(x);
        if (knowTransformed) {
            Term y = x.transform(d);
            if (y!=null && !y.equals(x) && !(y instanceof Bool))
                knowAmbient(y);
        }
    }

    void knowDerivedAbsolute(Subst d, Term x, long start, long end) {
        if (x.op() == IMPL && (!fullyEternal())) {
            //only know an impl as ambient if there is already non-eternal events detected
            knowDerivedAmbient(d, x);
            return;
        }

        knowAbsolute(x, start, end);

        if (knowTransformed) {
            Term y = x.transform(d);
            if (y!=null && !y.equals(x) && !(y instanceof Bool)) {
                knowAbsolute(y, start, end);
            }
        }
    }

    /** HACK HACK */
    private final static Set<Term> INDUCTION = Set.of(
            $.$safe("Induction"), $.$safe("InductionN"),
            $.$safe("InductionPN"),$.$safe("InductionNN"));

    @Nullable
    public Term solve(Conclusion c, @NotNull Derivation d, Term pattern, long[] occ, float[] confGain) {


        Task belief;
        if (d.single) {
            belief = null;
            constraints = sng;
        } else {
            belief = this.belief;
            constraints = dbl;

            //HACK HACK HACK special case: a repeat in temporal induction
            Op po = pattern.op();
            Term tt = task.term();
            if (po.temporal && !task.isEternal() && !belief.isEternal() && belief.term().equals(tt)) {
                if (((po == CONJ || po == IMPL) && pattern.subs() == 2)) {
                    Term p0 = pattern.sub(0);
                    if (p0.unneg().equals(tt) && p0.unneg().equals(pattern.sub(1).unneg())) {
                        occ[0] = task.start();

                        int dt = (int) (belief.start() - task.end());

                        //HACK HACK HACK
                        Term beliefTruth = c.rule.POST[0].beliefTruth;
                        boolean reverse = false;
                        if (po == IMPL && INDUCTION.contains(beliefTruth)) {
                            reverse = true;
                            occ[0] = belief.start();
                        } else if (po == CONJ) {
                            if (belief.isNegative() && belief.isAfter(task.start()))
                                reverse = true;
                        }

                        if (reverse) {
                            dt = -dt;
                        }

                        return pattern.dt(dt);
                    }
                }
            }

        }

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
        if (e == null) {
            return null;
        }
        Op eop = e.term.op();
        if (!eop.conceptualizable) {
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
                Interval common = Interval.intersect(ts, task.end(), belief.start(), belief.end());
                if (common == null)
                    return null;
                occ[0] = common.a;
                occ[1] = common.b;
                float overlapFactor = (1f + (common.b - common.a)) / (1 + Math.max(task.range(), belief.range()));
                confGain[0] *= overlapFactor;

//                //interpolate
//                ts = task.nearestTimeBetween(belief.start(), belief.end());
//                long bs = belief.nearestTimeBetween(ts, task.end());
//                if (ts != bs) {
//                    //confidence decay in proportion to lack of coherence
//                    if (task.isBeliefOrGoal()) {
//                        float taskEvi = task.evi();
//                        float beliefEvi = belief.evi();
//                        float taskToBeliefEvi = taskEvi / (taskEvi + beliefEvi);
//                        k = Util.lerp(taskToBeliefEvi, bs, ts); //TODO any duration?
//                        long distSum =
//                                Math.abs(task.nearestTimeTo(k) - k) +
//                                        Math.abs(belief.nearestTimeTo(k) - k);
//                        if (distSum > 0) {
//                            eviGain[0] *= Param.evidenceDecay(1, d.dur, distSum);
//                        }
//                    } else {
//                        k = bs;
//                    }
//                } else {
//                    k = ts;
//                }
            } else if (te) {
                occ[0] = belief.start();
                occ[1] = belief.end();
            } else /*if (be)*/ {
                occ[0] = occ[1] = task.start(); //TODO any duration?
            }

        }


        return e.term;
    }

}
