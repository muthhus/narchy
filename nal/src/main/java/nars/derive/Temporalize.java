package nars.derive;

import jcog.list.FasterList;
import jcog.math.Interval;
import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Derivation;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import nars.term.subst.Subst;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.time.Tense.*;

/**
 * set missing temporal relations in a derivation using constraint solver
 *
 * @see http://choco-solver.readthedocs.io/en/latest/1_overview.html#directly
 */
public class Temporalize {

    final static Logger logger = LoggerFactory.getLogger(Temporalize.class);

    /**
     * constraint graph
     */
    final Map<Term, FasterList<Event>> constraints = new HashMap<>();
    final Random random;
    public int dur = 1;

    /**
     * for testing
     */
    public Temporalize() {
        this(new XorShift128PlusRandom(1));
    }

    Temporalize(Random random) {
        this.random = random;
    }

    /**
     * heuristic for ranking temporalization strategies
     */
    float score(Term x) {
        FasterList<Event> l = constraints.get(x);
        if (l == null) {
            return Float.NEGATIVE_INFINITY;
        } else {
            float s = 0;
            for (int i = 0, lSize = l.size(); i < lSize; i++) {
                Event e = l.get(i);
                if (e instanceof AbsoluteEvent) {
                    if (((AbsoluteEvent) e).start != ETERNAL)
                        s += 3 * (1 + x.size()); //prefer non-eternal as it is more specific
                    else
                        s += 2;
                } else {
                    Term tr = ((RelativeEvent) e).rel;

//                    if (tr.op() == NEG) //NEG relations are not trustable
//                        s += 0.1;
//                    else
                    s += (1f / (1 + tr.size()));  //decrease according to the related term's size
                }
            }
            return s;
        }
    }

    AbsoluteEvent newAbsolute(Term x, long start, long end) {
        return new AbsoluteEvent(x, start, end);
    }

    public abstract class Event implements Comparable<Event> {

        public final Term term;

        Event(Term term) {
            this.term = term;
        }

        @Nullable
        abstract public Time start(Map<Term, Time> trail);

        @Nullable
        abstract public Time end(Map<Term, Time> trail);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Event)) return false;

            Event event = (Event) o;

            return term.equals(event.term);
        }

        @Override
        public int hashCode() {
            return term.hashCode();
        }

        /**
         * return a new instance with the term negated
         */
        abstract public Event neg();


        @Override
        public int compareTo(@NotNull Temporalize.Event that) {
            if (this == that) return 0;

            if (getClass() == that.getClass()) {
                //same class

                if (this instanceof RelativeEvent) {
                    RelativeEvent THIS = (RelativeEvent) this;
                    Term x = THIS.rel.term();
                    RelativeEvent THAT = (RelativeEvent) that;
                    Term y = THAT.rel.term();
                    if (x.equals(y)) {
                        int c1 = Integer.compare(THIS.start, THAT.start);
                        if (c1 != 0)
                            return c1;
                        return Integer.compare(THIS.end, THAT.end);
                    } else {

                        float xs = score(x);
                        float ys = score(y);
                        if (xs != ys) {
                            return Float.compare(ys, xs);
                        } else {
                            //prefer lower volume
                            int xv = x.volume();
                            int yv = y.volume();
                            if (xv == yv)
                                return x.compareTo(y);
                            else
                                return Integer.compare(xv, yv);
                        }
                    }

                } else if (this instanceof AbsoluteEvent) {
                    AbsoluteEvent THIS = (AbsoluteEvent) this;
                    AbsoluteEvent THAT = (AbsoluteEvent) that;
                    long sThis = THIS.start;
                    long sThat = THAT.start;

                    //eternal should be ranked lower
                    if (sThis == ETERNAL) return +1;
                    if (sThat == ETERNAL) return -1;

                    int cs = Long.compare(sThis, sThat);

                    if (cs != 0)
                        return cs;
                    return Long.compare(THIS.end, THAT.end);

                }

            } else {
                //different types: absolute vs. relative or relative vs. absolute

                //absolute eternal is dead last, even compared to relative
                if (this instanceof AbsoluteEvent) {
                    if (((AbsoluteEvent) this).start == ETERNAL)
                        return +1;
                }
                if (that instanceof AbsoluteEvent) {
                    if (((AbsoluteEvent) that).start == ETERNAL)
                        return -1;
                }

                if (this instanceof AbsoluteEvent)
                    return -1;

            }

            return +1;
        }

    }

    int dt(Event a, Event b, Map<Term, Time> trail) {

//        if (a instanceof AbsoluteEvent || b instanceof AbsoluteEvent) {
//            //at least one or both are absolute, forming a valid temporal grounding
//            Time ae = a.end(trail);
////            Time A = a.start(trail);
////            Time B = b.end(trail);
//            Time bs = b.start(trail);
//
//            int dt = dt(ae, bs);
//            //int dt = dt(A, B);
//
////            if (dt != DTERNAL) {
////                int shrink = dt(A, ae) + dt(bs, B);
////                if (dt < 0)
////                    dt += shrink;
////                else //if (dt >= 0)
////                    dt -= shrink;
////            }
//
//            return dt;
//
//            //return dt(a.start(times), b.end(times));
//        }

        if (a instanceof RelativeEvent && b instanceof RelativeEvent) {
            RelativeEvent ra = (RelativeEvent) a;
            RelativeEvent rb = (RelativeEvent) b;

            if (ra.rel.equals(rb.rel)) { //easy case
//                if (rb.end >= ra.start)
//                    return rb.end - ra.start;
//                else

                return rb.start - ra.end;

            }
            if (ra.rel.equals(rb.term) && rb.rel.equals(ra.term)) {
                //circular dependency: choose either, or average if they are opposite polarity
                int forward = -ra.end;
                int reverse = rb.start;
                if (Math.signum(forward) == Math.signum(reverse)) {
                    return (forward + reverse) / 2;
                }
            }

            int d = dt(a.end(trail), b.start(trail));
            if (d == DTERNAL)
                return DTERNAL;
            else {
                assert(d!=XTERNAL);

                int ar = ra.rel.subtermTime(ra.term);
                if (ar == DTERNAL)  ar = 0; //promote to dt=0
                if (ar == XTERNAL)  return XTERNAL;

                int br = rb.rel.subtermTime(rb.term);
                if (br == DTERNAL)  br = 0; //promote to dt=0
                if (br == XTERNAL)  return XTERNAL;

                return d
                        + br
                        - ar
                        ;
            }


//            Event rea = solve(ra.rel, trail);
//            if (rea!=null) {
//                Event reb = solve(rb.rel, trail);
//                if (reb!=null) {
//
//                    @Nullable Time s = rea.end(trail);
//                    @Nullable Time e = reb.start(trail);
//                    return dt(s, e)
//                            + rb.rel.subtermTime(rb.term)
//                            - ra.rel.subtermTime(ra.term)
//                            ; //TODO include offsets in ra and rb
//                }
//            }
        }

        return dt(a.end(trail), b.start(trail));

        //return XTERNAL;
    }

    static long[] intersect(Event a, Event b, Map<Term, Time> trail) {
        Time as = a.start(trail);
        if (as != null) {
            Time bs = b.start(trail);
            if (bs != null) {
                if (as.base == ETERNAL && bs.base == ETERNAL) {
                    return Tense.ETERNAL_RANGE;
                } else if (as.base != ETERNAL && bs.base != ETERNAL) {
                    Time ae = a.end(trail);
                    Time be = b.end(trail);
                    Interval ab = Interval.intersect(as.base, ae.base, bs.base, be.base);
                    if (ab != null) {
                        return new long[]{ab.a, ab.b};
                    } else {
                        //
                    }
                } else {
                    //one is eternal, the other isn't. use the non-eternal range
                    long start, end;
                    if (as.base == ETERNAL) {
                        start = bs.base;
                        end = b.end(trail).base;
                    } else {
                        start = as.base;
                        end = a.end(trail).base;
                    }
                    return new long[]{start, end};
                }
            }
        }
        return null;
    }

    static int dt(Time a /* from */, Time b /* to */) {

        assert (a.base != XTERNAL);
        assert (b.base != XTERNAL);

        if (a.base != ETERNAL && b.base != ETERNAL) {
            return (int) (b.abs() - a.abs()); //TODO check for numeric precision loss
        } else if (a.offset != XTERNAL && b.offset != XTERNAL && a.offset != DTERNAL && b.offset != DTERNAL) {
            //if (a.base == ETERNAL || b.base == ETERNAL) {
            return b.offset - a.offset; //relative offsets within a complete or partial eternal context
//            } else {
//                if (a.offset == b.offset)
//                    return a.offset;
//
//
//            }
        }
        throw new UnsupportedOperationException(a + " .. " + b); //maybe just return DTERNAL
    }

    class AbsoluteEvent extends Event {

        public final long start, end;

        AbsoluteEvent(Term term, long start, long end) {
            super(term);

            if (start == ETERNAL) {
                this.start = this.end = ETERNAL;
            } else {

                //int tdt = term.dtRange();

                if (end == ETERNAL) {
                    end = start;
                }

                long te;
                if (start <= end) {
                    this.start = start;
                    te = end;
                } else {
                    this.start = end;
                    te = start;
                }

                this.end = te;
            }
        }

//        @Override
//        public void apply(Map<Term, Time> trail) {
//            trail.put(term, Time.the(start, 0)); //direct set
//        }

        @Override
        public Event neg() {
            return new AbsoluteEvent($.neg(term), start, end);
        }

        @Override
        public Time start(@Nullable Map<Term, Time> ignored) {
//            if(ignored!=null) {
//                Time existingTime = ignored.get(term);
//                if (existingTime != null) {
//                    System.out.println("conflict?: " + existingTime + " " + Time.the(start, 0));
//                }
//            }
            return Time.the(start, 0);
        }

        @Override
        public Time end(Map<Term, Time> ignored) {
//            int dt = term.dt();
//            if (dt == DTERNAL)
//                dt = 0;
            return Time.the(end, 0);
        }

        @Override
        public String toString() {
            if (start != ETERNAL) {
                if (start != end)
                    return term + ("@[" + timeStr(start) + ".." + timeStr(end)) + ']';
                else
                    return term + "@" + timeStr(start);
            } else
                return term + "@ETE";
        }

    }

    static String timeStr(long when) {
        return when != ETERNAL ? Long.toString(when) : "ETE";
    }

    public SolutionEvent solution(Term term, Time start) {
        long st = start.abs();
        long et;
        if (st == ETERNAL) et = ETERNAL;
        else et = term.op() == CONJ ? st + term.dtRange() : st;

        return new SolutionEvent(term, st, et);
    }

    public class SolutionEvent extends AbsoluteEvent {


        SolutionEvent(Term term, long start) {
            this(term, start, start != ETERNAL ? start + term.dtRange() : ETERNAL);
        }

        SolutionEvent(Term term, long start, long end) {
            super(term, start, end);
        }

//        SolutionEvent(Term unknown) {
//            super(unknown, XTERNAL, XTERNAL);
//        }
    }

    /**
     * used for preserving an offset within an eternal context
     */
    static class Time {

        //        public static Time Unknown = new Time(ETERNAL, XTERNAL);
        public final long base;
        public final int offset;

        @Override
        public String toString() {
            return str(base) + '|' + str(offset);
        }

        static String str(int offset) {
            if (offset == XTERNAL)
                return "+-";
            else
                return Integer.toString(offset);
        }

        static String str(long base) {
            if (base == ETERNAL)
                return "ETE";
            else
                return Long.toString(base);
        }


        static Time the(long base, int offset) {
//            if (base == ETERNAL && offset == XTERNAL)
//                return Unknown;
//            else {

            if (base != ETERNAL && offset != DTERNAL && offset != XTERNAL)
                return new Time(base + offset, 0); //direct absolute
            else
                return new Time(base, offset);
//            }
        }

        private Time(long base, int offset) {
            this.base = base;
            this.offset = offset;
        }


        public Time add(int offset) {

            if (offset == 0)
                return this;

            if (this.offset == DTERNAL && offset == DTERNAL)
                return this; //no effect, adding dternal to dternal

            assert (this.offset != DTERNAL && offset != DTERNAL) :
                    "this.base=" + this.base + ", this.offset=" + this.offset + " + " + offset + " = ?";

            if (this.offset == XTERNAL)
                return Time.the(base, offset); //set initial dt
            else
                return Time.the(base, this.offset + offset);
        }

        public long abs() {
//            if (base == ETERNAL) {
//                return ETERNAL;
//            }
//
//            assert(offset!=XTERNAL);
//            assert(offset!=DTERNAL);
//            return base + offset;
            if (base != ETERNAL) {
                if (offset == XTERNAL || offset == DTERNAL)
                    return base;
                else
                    return base + offset;
            } else
                return ETERNAL;
        }
    }

    static String timeStr(int when) {
        return when != DTERNAL ? (when != XTERNAL ? Integer.toString(when) : "?") : "DTE";
    }

    RelativeEvent newRelative(Term term, Term relativeTo, int start) {
        return new RelativeEvent(term, relativeTo, start);
    }

    class RelativeEvent extends Event {
        private final Term rel;
        private final int start, end;

        RelativeEvent(Term term, Term relativeTo, int start) {
            this(term, relativeTo, start, start + term.dtRange());
        }

        RelativeEvent(Term term, Term relativeTo, int start, int end) {
            super(term);
            //assert (!term.equals(relativeTo));
            this.rel = relativeTo;
            this.start = start;
            this.end = end;
        }


//        @Override
//        public void apply(Map<Term, Time> trail) {
//            Time t = resolve(this.start, trail);
//            if (t != null)
//                trail.putIfAbsent(term, t); //direct set
//        }

        @Override
        public Event neg() {
            return new RelativeEvent($.neg(term), rel, start, end);
        }

        @Override
        @Nullable
        public Time start(Map<Term, Time> trail) {
            return resolve(this.start, trail);
        }

        @Override
        public Time end(Map<Term, Time> trail) {
            return resolve(this.end, trail);
        }

        @Nullable
        private Time resolve(int offset, Map<Term, Time> trail) {

            Event e = solve(rel, trail);
            if (e != null) {
                @Nullable Time rt = e.start(trail);
                if (rt != null)
                    return rt.add(offset);
            }

            return null;
        }

        @Override
        public String toString() {
            if (start != end) {
                return term + "@[" + timeStr(start) + ".." + timeStr(end) + "]->" + rel;
            } else {
                return term + "@" + timeStr(start) + "->" + rel;
            }
        }

    }


    protected void print() {

        constraints.forEach((k, v) -> {
            for (Event vv : v)
                System.out.println(k + " " + vv);
        });

        System.out.println();
    }

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
     */
    @Nullable
    public static Term solve(@NotNull Derivation d, Term pattern, long[] occ) {


        Task task = d.task;
        Task belief = d.belief;

        Temporalize model = new Temporalize(d.random);
        model.dur = Param.DITHER_DT ? d.dur : 1;

        boolean taskRooted = true; //(belief == null) || ( !task.isEternal() );
        boolean beliefRooted = true; //belief!=null && (!taskRooted || !belief.isEternal());


        model.know(task, d, taskRooted);

        if (belief != null) {
            if (!belief.equals(task)) {

//                if (task.isEternal() && belief.isEternal() /*&& some interesction of terms is prsent */)
//                    beliefRooted = false; //avoid confusing with multiple eternal roots; force relative calculation

                model.know(belief, d, beliefRooted); //!taskRooted || !belief.isEternal()); // || (bo != IMPL));
            }
        } else if (d.beliefTerm != null) {
            if (!task.term().equals(d.beliefTerm)) //dont re-know the term
                model.know(d.beliefTerm, d, null);
        }

        Map<Term, Temporalize.Time> trail = new HashMap<>();
        Event e;
        try {
            e = model.solve(pattern, trail);
        } catch (StackOverflowError er) {
            logger.error("temporalize stack overflow:\n{} {}\n\t{}\n\t{}", pattern, d, model, trail);
//            trail.clear();
//            model.solve(pattern, trail);
            return null;
        }
        if (e == null)
            return null;

        if (e instanceof AbsoluteEvent) {
            AbsoluteEvent a = (AbsoluteEvent) e; //faster, preferred since pre-calculated
            occ[0] = a.start;
            occ[1] = a.end;
        } else {
            occ[0] = e.start(trail).abs();
            occ[1] = e.end(trail).abs();
        }

        if (!(
                (occ[0] != ETERNAL)
                        ||
                        (task.isEternal()) && (belief == null || belief.isEternal()))) {
            //"eternal derived from non-eternal premise:\n" + task + ' ' + belief + " -> " + occ[0];
            return null;
        }
        return e.term;
    }

    void know(Task task, Subst d, boolean rooted) {
        Term taskTerm = task.term();

        AbsoluteEvent root =
                (rooted) ?
                        new AbsoluteEvent(taskTerm, task.start(), task.end())
                        :
                        null;

        know(taskTerm, d, root);
    }

    void know(Term term, Subst d, @Nullable AbsoluteEvent root) {
        know(term, root);

        Term t2 = d.transform(term);
        if (!t2.equals(term)) {
           know(t2, root);
        }
    }

    private void know(Term term, @Nullable Temporalize.AbsoluteEvent root) {
        int d = term.dtRange();
        if (root!=null && (term.op() == CONJ && (root.end - root.start!= d)))
            return; //as a result of variables etc, the conjunction has been resized; the numbers may not be compareable so to be safe, cancel
        know(root, term, 0, d);
    }


    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    public Temporalize knowTerm(Term term, long when) {
        return knowTerm(term, when, when != ETERNAL ? when + term.dtRange() : ETERNAL);
    }

    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    Temporalize knowTerm(Term term, long from, long to) {
        know(new AbsoluteEvent(term, from, to), term,
                0, (int) (to - from)
        );

        return this;
    }


    /**
     * recursively calculates the start and end time of all contained events within a term
     *
     * @param occ    superterm occurrence, may be ETERNAL
     * @param start, end - term-local temporal bounds
     */
    void know(@Nullable Event parent, Term term, int start, int end) {

//        if (term instanceof Variable) // || (!term.hasAny(ATOM.bit | INT.bit)))
//            return; //ignore variable's and completely-variablized's temporalities because it can conflict

        //TODO support multiple but different occurrences  of the same event term within the same supercompound
        if (parent == null || parent.term != term) {
            List<Event> exist = constraints.get(term);
            if (exist != null)
                return;
        }

        if (parent != null)
            add(parent, term, start, end);


        Op o = term.op();
        if (o.temporal) {
            int dt = term.dt();

            if (dt == XTERNAL) {

                //TODO UNKNOWN TO SOLVE FOR
                //throw new RuntimeException("no unknowns may be added during this phase");

            } else {
                if (dt == DTERNAL)
                    dt = 0;

                boolean reverse;
                TermContainer tt = term.subterms();
                if (dt!=DTERNAL && dt < 0 && o.commutative) {
                    dt = -dt;
                    tt = tt.reverse();
                    reverse = true;
                } else {
                    reverse = false;
                }

                int t = start;

                int l = tt.size();

                //System.out.println(tt + " presubs " + t + "..reverse=" + reverse);
                int lastSubStart = DTERNAL;
                for (int i = 0; (i < l); i++) {

                    Term st = tt.sub(i);
                    int sdt = /*o == CONJ ? */st.dtRange();/* : 0*/ /* dont count internal event dtRange if not in CONJ */

                    int subStart = t;
                    int subEnd = t + sdt;


//                  System.out.println(parent + "\t" + st + " sub(" + i + ") " + subStart + ".." + subEnd);
                    know(parent, st, subStart, subEnd);



                    t = subEnd; //the duration of the event

                    if (i < l - 1)
                        t += dt; //the dt offset (doesnt apply to the first term which is early/left-aligned)

                    if (i > 0) {
                        //crosslink adjacent subterms
                        add(tt.sub(i - 1), new RelativeEvent(tt.sub(i - 1), tt.sub(i), lastSubStart - subStart));
                        add(tt.sub(i), new RelativeEvent(tt.sub(i), tt.sub(i - 1), subStart - lastSubStart));
                    }
                    lastSubStart = subStart;
                }


//                    //for conjunctions: by the end of the iteration we should be at the exact end of the interval
//                    if (o == CONJ) {
//                        int expectedEnd = end;
////                        if (t!=expectedEnd) {
////                            throw new RuntimeException(term + " with dtRange=" + term.dtRange() + " mis-aligned: " + start + "," + end + " but t=" + t);
////                        }
//                        assert (t == expectedEnd) : term + " with dtRange=" + term.dtRange() + " mis-aligned: " + start + "," + end + " but t=" + t;
//                    }

                //for others: they are "pointers" which relate time points but do not define durations

            }

        } else {
            //all these subterms will share their supercompounds time
//                if (o.isSet() ) {
//                    c.subterms().forEach(s -> know(root, s, start, end));
//                }

                /*c.subterms().recurseTerms((s) -> {
                    know(root, s, start, end);
                });*/
        }
    }


    void add(@NotNull Temporalize.Event root, Term term, int start, int end) {
        Event event;

        if (term.equals(root.term)) {
            event = root;
        } else {
            Time occ = root.start(null);
            assert (occ.base != XTERNAL);
            event = (occ.base != ETERNAL ?
                    new AbsoluteEvent(term, occ.abs() + start, occ.abs() + end) :
                    new RelativeEvent(term, root.term, start, end)
            );
        }

        add(term, event);
    }


    void add(Term term, Event event) {

        FasterList<Event> l = constraints.computeIfAbsent(term, (t) -> new FasterList<>());
        l.add(event);

        if (term.op() == NEG) {
            Term u = term.unneg();
            FasterList<Event> m = constraints.computeIfAbsent(u, (t) -> new FasterList<>());
            m.add(new RelativeEvent(u, term, 0, term.dtRange()));
            if (m.size() > 1)
                m.sortThis();
        }

        if (l.size() > 1)
            l.sortThis();
    }

    /** warning: for external use only; all internal calls should use solve(target, trail) to prevent stack overflow */
    public Event solve(Term target) {
        return solve(target, new HashMap<>(target.volume()));
    }


    Event solve(final Term x, Map<Term, Time> trail) {

        //System.out.println("solve " + target + "\t" + trail);

        if (trail.containsKey(x)) {
            Time xs = trail.get(x);
            if (xs != null)
                return solution(x, xs);
            else
                return null; //cyclic
        }

        trail.put(x, null); //placeholder

        FasterList<Event> cc = constraints.get(x);
        if (cc != null) {

            int ccc = cc.size();
            assert (ccc > 0);
            if (ccc > 1)
                cc.sortThis();

            for (int i = 0, eaSize = ccc; i < eaSize; i++) {
                Event e = cc.get(i);

                //System.out.println(x + " " + i + "\t" + trail + "\t" + e);

                Time xs = e.start(trail);
                if (xs != null) {
                    trail.put(x, xs);
                    return e; //solution(x, xs);
                }
            }
        }

        Event e = solveComponents(x, trail);

        if (e != null) {
            Time xs = e.start(trail);
            if (xs != null) {
                trail.put(x, xs); //assign
                return e;
            }
        }

        trail.remove(x); //remove placeholder
        return null;
    }

    private Event solveComponents(Term target, Map<Term, Time> trail) {

        Op o = target.op();

        if (o == NEG) {
            Event ss = solve(target.unneg(), trail);
            if (ss != null)
                return ss.neg();
            else
                return null;
        } else if (o.temporal && target.dt() == XTERNAL) {
            TermContainer tt = target.subterms();

            int tts = tt.size();
            assert (tts > 1);
            if (tts == 2) {


                boolean dir = true; //forward
                Term t1 = tt.sub(1);
                Term t0 = tt.sub(0);

                //decide subterm solution order intelligently: allow reverse if the 2nd subterm can more readily and absolutely temporalize
                if (score(t1) > score(t0)  /* || t1.volume() > t0.volume()*/) {
                    dir = false; //reverse: solve simpler subterm first
                }

                Event ea, eb;
                if (dir) {
                    //forward
                    if ((ea = solve(t0, trail)) == null)
                        return null;
                    if ((eb = solve(t1, trail)) == null)
                        return null;
                } else {
                    //reverse
                    if ((eb = solve(t1, trail)) == null)
                        return null;
                    if ((ea = solve(t0, trail)) == null)
                        return null;
                }


                Time at = ea.start(trail);

                if (at != null) {

                    Time bt = eb.start(trail);

                    if (bt != null) {

                        Term a = ea.term;
                        Term b = eb.term;

                        try {
                            if (o == CONJ && (a.op() == CONJ || b.op() == CONJ)) {
                                //conjunction merge, since the results could overlap
                                //either a or b, or both are conjunctions. and the result will be conjunction

                                Event e = solveConj(at, bt, a, b);
                                if (e != null)
                                    return e;

                            } else {

                                Event e = solveTemporal(trail, o, ea, eb, a, b);
                                if (e != null)
                                    return e;

                            }
                        } catch (UnsupportedOperationException e) {
                            logger.warn("temporalization solution: {}", e.getMessage());
                            return null; //TODO
                        }
                    }
                }
            }
        }


        /** compute the temporal intersection of all involved terms. if they are coherent, then
         * create the solved term as-is (since it will not contain any XTERNAL) with the appropriate
         * temporal bounds. */

        if (o.temporal) {
            TermContainer tt = target.subterms();
            int tts = tt.size();
            if (tts == 2) {

                Term a = tt.sub(0);
                Event ra = solve(a, trail);

                if (ra != null) {

                    Term b = tt.sub(1);
                    Event rb = solve(b, trail);

                    if (rb != null) {
                        return solveTemporal(trail, o, ra, rb, a, b);
                    }
                }
            } else {
                assert (tts > 2);

                /* HACK quick test for the exact appearance of temporalized form present in constraints */
                if (target.dt() == XTERNAL) {
                    Term[] a = target.subterms().toArray();
                    {
                        @NotNull Term d = target.op().the(DTERNAL, a);
                        if (!(d instanceof Bool)) {
                            Event ds = solve(d, trail);
                            if (ds != null)
                                return ds;
                        }
                    }
                    {
                        @NotNull Term d = target.op().the(0, a);
                        if (!(d instanceof Bool)) {
                            Event ds = solve(d, trail);
                            if (ds != null)
                                return ds;
                        }
                    }
                }

                Event s0 = solve(tt.sub(0), trail);
                if (s0 != null) {
                    Event s1 = solve(tt.sub(1), trail);
                    if (s1 != null) {
                        int dt = dt(s0, s1, trail);
                        if (dt == 0 || dt == DTERNAL) {
                            return new SolutionEvent(o.the(dt, tt.toArray()), s0.start(trail).abs());
                        } else {
                            return null; //invalid
                        }
                    }
                }
            }

        } else if (o.statement) {
            //choose two absolute events which cover both 'a' and 'b' terms
            List<Event> relevant = $.newArrayList(); //maybe should be Set?
            Set<Term> uncovered = new HashSet();
            target.subterms().recurseTermsToSet(
                    ~(Op.SECTi.bit | Op.SECTe.bit | Op.DIFFe.bit | Op.DIFFi.bit) /* everything but sect/diff; just their content */,
                    uncovered, true);

            for (Term c : constraints.keySet()) {

                if (c.equals(target)) continue; //cyclic; already tried above

                Event ce = solve(c, trail);

                if (ce != null) {
                    if (uncovered.removeIf(c::containsRecursively)) {
                        relevant.add(ce);
                        if (uncovered.isEmpty())
                            break; //got them all
                    }
                }

            }
            if (!uncovered.isEmpty())
                return null; //insufficient information

            //HACK just use the only two for now, it is likely what is relevant anyway
            int rr = relevant.size();
            switch (rr) {
                case 0:
                    return null; //can this happen?
                case 1:
                    Event r = relevant.get(0);
                    return new SolutionEvent(target, r.start(trail).abs(), r.end(trail).abs());

            }

            int retries = rr > 2 ? 2 : 1;

            for (int i = 0; i < retries; i++) {

                if (rr > 2)
                    Collections.shuffle(relevant, random); //dont reshuffle if only 2, it's pointless; intersection is symmetric

                Event ra = relevant.get(0);
                Event rb = relevant.get(1);

                Event ii = solveStatement(target, trail, ra, rb);
                if (ii != null)
                    return ii;
            }
        }

        return null;
    }

    private Event solveConj(Time at, Time bt, Term a, Term b) {
        long ata = at.abs();
        long bta = bt.abs();
        if (ata == ETERNAL && bta == ETERNAL) {
            ata = at.offset;
            bta = bt.offset;// + a.dtRange();
        } else if (ata == ETERNAL ^ bta == ETERNAL) {
            return null; //one is eternal the other isn't
        }
        Term newTerm = Op.conjMerge(a, ata, b, bta);
        long start = Math.min(at.abs(), bt.abs());
        return new SolutionEvent(newTerm, start);
    }

    private Event solveStatement(Term target, Map<Term, Time> trail, Event ra, Event rb) {
        long[] ii = intersect(ra, rb, trail);
        if (ii != null) {
            //overlap or adjacent
            return new SolutionEvent(target, ii[0], ii[1]);
        } else {
            //not overlapping at all, compute point interpolation
            Time as = ra.start(trail);
            if (as != null) {
                Time bs = rb.start(trail);
                if (bs != null) {
                    Time at = ra.end(trail);
                    if (at != null) {
                        Time bt = rb.end(trail);
                        if (bt != null) {
                            long ta = as.abs();
                            long tz = at.abs();
                            if (tz == ETERNAL) tz = ta;
                            long ba = bs.abs();
                            long bz = bt.abs();
                            if (bz == ETERNAL) bz = ba;
                            long dist = Interval.unionLength(ta, tz, ba, bz) - (tz - ta) - (bz - ba);
                            if (Param.TEMPORAL_TOLERANCE_FOR_NON_ADJACENT_EVENT_DERIVATIONS >= ((float) dist) / dur) {
                                long occ = ((ta + tz) / 2L + (ba + bz) / 2L) / 2L;
                                //occInterpolate(t, b);
                                return new SolutionEvent(target, occ);
                            }
                        }
                    }
                }
            }

        }
        return null;
    }

    private Event solveTemporal(Map<Term, Time> trail, Op o, Event ea, Event eb, Term a, Term b) {
        int dt = dt(ea, eb, trail);

//        int inner = a.dtRange();// + b.dtRange();
//        assert(Math.abs(dt) >= inner);
//        if (dt >= 0) dt -= inner;
//        else dt += inner;

        if (dt != 0 && Math.abs(dt) < dur)
            dt = 0; //perceived as simultaneous within duration

//                                        if (o == CONJ && sd != DTERNAL && sd != XTERNAL) {
//                                            sd -= a.dtRange(); sd -= b.dtRange();
//                                        }
        if (dt != XTERNAL) {

            @Nullable Time at = ea.start(trail);

            Term newTerm = o.the(dt, a, b);

            long start = at.abs();
            if (o == CONJ && start != ETERNAL && dt != DTERNAL) {
                Time bt = eb.start(trail);
                if (bt != null) {
                    long bStart = bt.abs();
                    if (bStart != ETERNAL) {
                        if (bStart < start)
                            start = bStart;
                    }
                }
            }

            return new SolutionEvent(newTerm, start);
        }
        return null;
    }

//    @Nullable
//    private Time solveTime(Term x, Map<Term, Time> trail) {
//
//        if (trail.containsKey(x)) {
//            Time t = trail.get(x);
//            if (t != null)
//                return t;
//            else
//                return null; //cyclic
//        }
//
//        Event e = solve(x, trail);
//        if (e != null) {
//            Time t2 = e.start(trail);
//            trail.put(x, t2);
//            return t2;
//        } else {
//            return null;
//        }
//
//    }


    @Override
    public String toString() {
        return constraints.values().stream().flatMap(Collection::stream).map(Object::toString).collect(Collectors.joining(","));
    }


}
