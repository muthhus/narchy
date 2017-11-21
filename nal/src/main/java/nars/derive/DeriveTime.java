package nars.derive;

import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.derive.time.TimeGraph;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.subst.Subst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;


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
public class DeriveTime extends TimeGraph {

    private final static Logger logger = LoggerFactory.getLogger(DeriveTime.class);

    private final Task task, belief;

    protected static final boolean knowTransformed = true;
    private final int dur;
    private final Derivation d;


    public DeriveTime(Derivation d, boolean single) {
        this.d = d;
        this.task = d.task;
        this.belief = d.belief; //!d.single ? d.belief : null;
        this.dur = Math.max(1, Math.round(d.nar.dtDither.floatValue() * d.dur));

        long taskStart = task.start();

        //Term taskTerm = polarizedTaskTerm(task);
        know(d, task, taskStart);


        if (!single && belief != null && !belief.equals(task)) {

            long beliefStart = belief.start();

            //Term beliefTerm = polarizedTaskTerm(belief);
            know(d, belief, beliefStart);

        } else if (!task.term().equals(d.beliefTerm)) {
            know(d, d.beliefTerm, TIMELESS);
        }

    }

    public Term solve(Term pattern) {

//        if (taskStart == ETERNAL && task.isGoal() && belief!=null && !belief.isEternal()) {
//            //apply this as a temporal goal task at the present time, since present time does occur within the eternal task
//            taskStart = taskEnd = d.time;
//        }

        long[] occ = d.concOcc;

        Term tt = task.term();
        Term bb = d.beliefTerm;


        if (d.single) {
            //single

            if (!tt.isTemporal()) {
                //simple case: inherit task directly
                d.concOcc[0] = task.start();
                d.concOcc[1] = task.end();
                return pattern;
            }


        } else {
            //double

        }


        Event[] best = new Event[1];

        final int[] remain = {8};

//        try {


        solve(pattern, false /* take everything */, (solution) -> {

            //TODO test equivalence with task and belief terms and occurrences, and continue iterating up to a max # of tries if it produced a useless equivalent result

            Event current = best[0];
            if (current == null) {
                best[0] = current;
            } else {
                best[0] = merge(best[0], solution);
            }
            return remain[0]-- > 0;
        });


//        } catch (Throwable t) {
//            logger.error("temporalize error:\n{} {}\n\t{}", pattern, d, t);
//            return null;
//        }

        Event event = best[0];
        if (event == null)
            return null;

        Term st = event.id;
        Op eop = st.op();
        if (!eop.conceptualizable) {
            return null;
        }

        occ[0] = event.start();
        occ[1] = event.end();

        boolean te = task.isEternal();
        if (occ[0] == TIMELESS || occ[0] == ETERNAL) {
            //couldnt solve the start time, so inherit from task or belief as appropriate
            if (!te || belief == null || belief.isEternal()) {
                occ[0] = occ[1] = task.start();
            } else if (belief != null) {
                occ[0] = occ[1] = belief.start();
            } else {
                if (occ[0] == TIMELESS)
                    return null; //no basis
                else {
                    //its eternal
                }
            }
        }

//        if (occ[0] == ETERNAL && (!te || (belief != null && !belief.isEternal()))) {
//            //"eternal derived from non-eternal premise:\n" + task + ' ' + belief + " -> " + occ[0];
//            //uneternalize/retemporalize:
//
////            if (/*(e.term.op() != IMPL) && */
////                    (task.op() == IMPL) && (belief == null || d.beliefTerm.op() == IMPL)) {
////                //dont retemporalize a non-implication derived from two implications
////                //it means that the timing is unknown
////                return null;
////            }
//
//            long ts = task.start();
//            long k;
//            if (!te && (belief != null && !belief.isEternal())) {
//
//                Interval ii = Interval.intersect(ts, task.end(), belief.start(), belief.end());
//                if (ii == null)
//                    return null;
//                occ[0] = ii.a;
//                occ[1] = ii.b;
//
////                Revision.TaskTimeJoint joint = new Revision.TaskTimeJoint(ts, task.end(), belief.start(), belief.end(), d.nar);
////                occ[0] = joint.unionStart;
////                occ[1] = joint.unionEnd;
////                d.concConfFactor *= joint.factor;
//
//            } else if (te) {
//                //TODO maybe this should be 'now'
//                //occ[0] = belief.start();
//                //occ[1] = belief.end();
//                occ[0] = belief.start();
//                occ[1] = belief.end();
//            } else /*if (be)*/ {
//                occ[0] = task.start();
//                occ[1] = task.end();
//            }
//
//        }
//
//        if (occ[0] != ETERNAL && occ[1] < occ[0]) {
//            if (occ[1] == ETERNAL) {
//                occ[1] = occ[0];
//            } else {
//                //HACK swap for order
//                long t = occ[0];
//                occ[0] = occ[1];
//                occ[1] = t;
//            }
//        }

        return st;
    }

    /** heuristic for deciding a derivation result from among the calculated options */
    protected Event merge(Event a, Event b) {
        Term at = a.id;
        Term bt = b.id;
        if (at.hasXternal() && !bt.hasXternal())
            return b;

        long astart = a.start();
        long bstart = b.start();
        if (astart == TIMELESS && bstart!=TIMELESS)
            return b;
        if (astart == ETERNAL && bstart!=ETERNAL)
            return b;

        //heuristic: prefer more specific "dense" temporal events rather than sprawling sparse run-on-sentences
        float aSpec = ((float)at.volume())/at.dtRange();
        float bSpec = ((float)bt.volume())/bt.dtRange();
        if (bSpec > aSpec)
            return b;
        else
            return a;

//        Term tRoot = at.root();
//        if (!at.equals(tt)) {
//            score++;
//            if (!tRoot.equals(tt.root()))
//                score++;
//        }
//
//        if (!at.equals(bb)) {
//            score++;
//            if (!tRoot.equals(bb.root()))
//                score++;
//        }

    }

//    /**
//     * negate if negated, for precision in discriminating positive/negative
//     */
//    static Term polarizedTaskTerm(Task t) {
//        Truth tt = t.truth();
//        return t.term().negIf(tt != null && tt.isNegative());
//    }

    @Override
    protected Random random() {
        return d.random;
    }

    void know(Subst d, Termed x, long start) {

        if (x instanceof Task)
            know((Task)x);
        else
            know(x.term());

        if (knowTransformed) {
            Term y = //x.transform(d);
                    x.term().eval(d);
            if (y != null && !y.equals(x) && !(y instanceof Bool)) {
                know(y, start);
            }
        }
    }


}
