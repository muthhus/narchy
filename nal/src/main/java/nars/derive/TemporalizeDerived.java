package nars.derive;

import nars.$;
import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.derive.time.AbsoluteEvent;
import nars.derive.time.Event;
import nars.derive.time.Temporalize;
import nars.derive.time.Time;
import nars.index.term.TermContext;
import nars.task.Revision;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.subst.Subst;
import nars.term.transform.Retemporalize;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

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
public class TemporalizeDerived extends Temporalize {

    private final static Logger logger = LoggerFactory.getLogger(TemporalizeDerived.class);

    private final Task task, belief;

    protected static final boolean knowTransformed = true;

    final HashMap trail = new LinkedHashMap();

    /**
     * constraints specific for specific double premise derivations
     */
    public final Map<Term, SortedSet<Event>> sng;
    public final Map<Term, SortedSet<Event>> dbl;

    public TemporalizeDerived(Derivation d) {
        super(d.random);
        task = d.task;
        belief = d.belief; //!d.single ? d.belief : null;
        dur = Math.max(1, Math.round(d.nar.dtDither.floatValue() * d.dur));

        long taskStart = task.start();
        long taskEnd = task.end();

//        if (taskStart == ETERNAL && task.isGoal() && belief!=null && !belief.isEternal()) {
//            //apply this as a temporal goal task at the present time, since present time does occur within the eternal task
//            taskStart = taskEnd = d.time;
//        }

        knowDerivedAbsolute(d,
                polarizedTaskTerm(task),
                taskStart, taskEnd);


        if (!task.term().equals(d.beliefTerm)) { //dont re-know the term

            Term b = d.beliefTerm;
            knowDerivedAmbient(d, b);

        }

        sng = constraints;

        if (belief != null && !belief.equals(task)) {

            this.constraints = dbl = new HashMap(); //clone
            sng.forEach((k, v) -> dbl.put(k, new TreeSet(v)));

            long beliefStart = belief.start();
            long beliefEnd = belief.end();

            if (belief.op().temporal && taskStart != ETERNAL) {
                beliefStart = beliefEnd = ETERNAL; //pretend as if belief is eternal, allowing task to override the result determination
            }

            knowDerivedAbsolute(d,
                    polarizedTaskTerm(belief),
                    beliefStart, beliefEnd); //!taskRooted || !belief.isEternal()); // || (bo != IMPL));


        } else {
            this.dbl = sng;
        }
    }

    /**
     * negate if negated, for precision in discriminating positive/negative
     */
    static Term polarizedTaskTerm(Task t) {
        Truth tt = t.truth();
        return t.term().negIf(tt != null && tt.isNegative());
    }

    void knowDerivedAmbient(TermContext d, Term x) {
        knowAmbient(x);
        if (knowTransformed) {
            Term y = //x.transform(d);
                    x.eval(d);
            if (!y.equals(x) && y.op().conceptualizable)
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
            Term y = //x.transform(d);
                    x.eval(d);
            if (y != null && !y.equals(x) && !(y instanceof Bool)) {
                knowAbsolute(y, start, end);
            }
        }
    }

    /**
     * HACK HACK
     */
    private final static Set<Term> INDUCTION = Set.of(
            $.$safe("Induction"), $.$safe("InductionN"),
            $.$safe("InductionPN"), $.$safe("InductionNN"));

    @Nullable
    public Term solve(Conclusion c, Derivation d, Term pattern) {

        long[] occ = d.concOcc;

        Task belief;
        Term tt = task.term();
        if (d.single) {
            belief = null;
            constraints = sng;

            if (!tt.isTemporal()) {
                //simple case
                d.concOcc[0] = task.start();
                d.concOcc[1] = task.end();
                return pattern;
            }

        } else {
            belief = this.belief;
            constraints = dbl;

            //HACK HACK HACK special case: a repeat in temporal induction
            Op po = pattern.op();

            if (po.temporal && !task.isEternal() && !belief.isEternal() && belief.term().unneg().equals(tt)) {
                if (/*((po == CONJ || po == IMPL) && */pattern.subs() == 2) {
                    Term p0 = pattern.sub(0).unneg();
                    if (p0.equals(tt) && p0.equals(pattern.sub(1).unneg())) {
                        occ[0] = task.start();


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

                        int dt;
                        if (reverse) {
                            dt = (int) (task.start() - belief.end());
                        } else {
                            dt = (int) (belief.start() - task.end());
                        }

                        return pattern.dt(dt);
                    }
                }
            }


        }

        Map<Term, Time> trail = this.trail;
        this.trail.clear();

        Event e;
        try {

            Predicate<Time> filter;
            if (task.term().equals(pattern)) {
                filter = (t) -> {
                    return t.abs() != task.start(); //find an event which differs from the that time
                };
            } else if (belief!=null && belief.term().equals(pattern)) {
                filter = (t) -> {
                    return t.abs() != belief.start(); //find an event which differs from the that time
                };
            } else {
                filter = t -> true;
            }

            e = solve(pattern, trail, filter);
            if (e == null) {
                return null;
            }
        } catch (StackOverflowError ignored) {
            logger.error("temporalize stack overflow:\n{} {}\n\t{}", pattern, d, trail);
//            trail.clear();
//            model.solve(pattern, trail);
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
                Revision.TaskTimeJoint joint = new Revision.TaskTimeJoint(ts, task.end(), belief.start(), belief.end(), d.nar);
                occ[0] = joint.unionStart;
                occ[1] = joint.unionEnd;
                d.concConfFactor *= joint.factor;

            } else if (te) {
                //TODO maybe this should be 'now'
                //occ[0] = belief.start();
                //occ[1] = belief.end();
                occ[0] = occ[1] = d.time;
            } else /*if (be)*/ {
                occ[0] = occ[1] = task.start(); //TODO any duration?
            }

        }

        if (occ[0]!=ETERNAL && occ[1] < occ[0]) {
            if (occ[1] == ETERNAL) {
                occ[1] = occ[0];
            } else {
                //HACK swap for order
                long t = occ[0];
                occ[0] = occ[1];
                occ[1] = t;
            }
        }

        return e.term;
    }

}
