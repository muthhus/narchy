package nars.derive.time;

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
import nars.term.var.Variable;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * set missing temporal relations in a derivation using constraint solver
 *
 * @see http://choco-solver.readthedocs.io/en/latest/1_overview.html#directly
 */
public class Temporalize implements ITemporalize {

    final static Logger logger = LoggerFactory.getLogger(Temporalize.class);

    /**
     * constraint graph
     */
    public final Map<Term, SortedSet<Event>> constraints = new HashMap<>();
    final Random random;
    public int dur = 1;

    /**
     * for testing
     */
    public Temporalize() {
        this(new XorShift128PlusRandom(1));
    }

    public Temporalize(Random random) {
        this.random = random;
    }

    @Override
    public @Nullable Term solve(@NotNull Derivation d, Term pattern, long[] occ) {
        dur = Param.DITHER_DT ? d.dur : 1;
        return ITemporalize.super.solve(d, pattern, occ);
    }

    /**
     * heuristic for ranking temporalization strategies
     */
    @Override
    public float score(Term x) {
        SortedSet<Event> cc = constraints.get(x);
        if (cc == null) {
            return Float.NEGATIVE_INFINITY;
        } else {
            float s = 0;
            for (Event e : cc) {
                float t;
                if (e instanceof AbsoluteEvent) {
                    if (((AbsoluteEvent) e).start != ETERNAL)
                        t = 2; // * (1 + x.size()); //prefer non-eternal as it is more specific
                    else
                        t = 0;
                } else if (e instanceof TimeEvent) {
                    //if (((TimeEvent)e).
                    t = 2;
                } else {
                    Term tr = ((RelativeEvent) e).rel;

                    if (tr.op() == NEG) //NEG relations are not that valuable
                        t = 0.1f;
                    else
                        t = 1;
                    //  (1f / (1 + tr.size()));  //decrease according to the related term's size

                }
                s = Math.max(s, t);
            }
            return s;
        }
    }

    public AbsoluteEvent absolute(Term x, long start, long end) {
        return new AbsoluteEvent(this, x, start, end);
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
                assert (d != XTERNAL);

                int ar = ra.rel.subtermTime(ra.term);
                if (ar == DTERNAL) ar = 0; //promote to dt=0
                if (ar == XTERNAL) return XTERNAL;

                int br = rb.rel.subtermTime(rb.term);
                if (br == DTERNAL) br = 0; //promote to dt=0
                if (br == XTERNAL) return XTERNAL;

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


    public Event solution(Term term, Time start) {
        return new TimeEvent(this, term, start);
//        long st = start.abs();
//        long et;
//        if (st == ETERNAL) et = ETERNAL;
//        else et = term.op() == CONJ ? st + term.dtRange() : st;
//
//        return new SolutionEvent(this, term, st, et);
    }

    static String timeStr(int when) {
        return when != DTERNAL ? (when != XTERNAL ? Integer.toString(when) : "?") : "DTE";
    }

    public RelativeEvent newRelative(Term term, Term relativeTo, int start) {
        return new RelativeEvent(this, term, relativeTo, start);
    }

    RelativeEvent relative(Term term, Term relativeTo, int start, int end) {
        return new RelativeEvent(this, term, relativeTo, start, end);
    }


    protected void print() {

        constraints.forEach((k, v) -> {
            for (Event vv : v)
                System.out.println(k + " " + vv);
        });

        System.out.println();
    }


    @Override
    public void know(Task task, Subst d, boolean rooted) {
        Term taskTerm = task.term();

        AbsoluteEvent root =
                (rooted) ?
                        absolute(taskTerm, task.start(), task.end())
                        :
                        null;

        know(taskTerm, d, root);
    }

    @Override
    public void know(Term term, Subst d, @Nullable AbsoluteEvent root) {
        know(term, root);

        Term t2 = d.transform(term);
        if (!t2.equals(term)) {
            know(t2, root);
        }
    }

    private void know(Term term, @Nullable AbsoluteEvent root) {
        int d = term.dtRange();
        if (root != null && (term.op() == CONJ && (root.end - root.start != d)))
            return; //as a result of variables etc, the conjunction has been resized; the numbers may not be compareable so to be safe, cancel
        know(root, term, 0, d);
    }


    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    public void knowTerm(Term term, long when) {
        knowTerm(term, when, when != ETERNAL ? when + term.dtRange() : ETERNAL);
    }

    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    public void knowTerm(Term term, long from, long to) {
        AbsoluteEvent basis = absolute(term, from, to);
        know(basis, term,
                0, (int) (to - from)
        );
    }


    /**
     * recursively calculates the start and end time of all contained events within a term
     *
     * @param occ    superterm occurrence, may be ETERNAL
     * @param start, end - term-local temporal bounds
     */
    void know(@Nullable Event parent, Term x, int start, int end) {

        if (x instanceof Variable) // || (!term.hasAny(ATOM.bit | INT.bit)))
            return; //ignore variable's and completely-variablized's temporalities because it can conflict

        //TODO support multiple but different occurrences  of the same event term within the same supercompound
        if (parent == null || parent.term != x) {
            SortedSet<Event> exist = constraints.get(x);
            if (exist != null)
                return;
        }

        if (parent != null) {
            Event event;

            if (x.equals(parent.term)) {
                event = parent;
            } else {
                Time occ = parent.start(null);
                assert (occ.base != XTERNAL);
                event = (occ.base != ETERNAL ?
                        absolute(x, occ.abs() + start, occ.abs() + end) :
                        relative(x, parent.term, start, end)
                );
            }

            know(x, event);
        } else {
            know(x, (Event) null);
        }


        Op o = x.op();
        if (o.temporal) {
            int dt = x.dt();

            if (dt == XTERNAL) {

                //TODO UNKNOWN TO SOLVE FOR
                //throw new RuntimeException("no unknowns may be added during this phase");

            } else {

                TermContainer tt = x.subterms();

                boolean reverse;
                if (dt == DTERNAL) {
                    dt = 0;
                    reverse = false;
                } else if (dt >= 0) {
                    reverse = false;
                } else {
                    reverse = true;
                    if (o == CONJ) {
                        tt = tt.reverse();
                        dt = -dt;
                    }
                }


                int l = tt.size();


                int t = start;
                int last = DTERNAL;

                //System.out.println(tt + " presubs " + t + "..reverse=" + reverse);
                for (int i = 0; (i < l); i++) {

                    Term st = tt.sub(i);

                    if (i > 0)
                        t += dt; //the dt offset (doesnt apply to the first term which is early/left-aligned)

                    int sdt = st.dtRange();
                    int subStart = t, subEnd = t + sdt;

                    //                  System.out.println(parent + "\t" + st + " sub(" + i + ") " + subStart + ".." + subEnd);

                    //the event is atomic, so forget the parent in computing the subterm relations (which is in IMPL only)
                    know(
                            (o != IMPL) ? parent : null, //(!term.op().statement) ? parent : null,
                            //parent,
                            st, subStart, subEnd); //parent = null;

                    t = subEnd; //the duration of the event


                    if (o == IMPL && i > 0) {
                        //IMPL: crosslink adjacent subterms.  conjunction is already temporalized in another method
                        int rel = last - subStart;
                        know(tt.sub(i - 1), newRelative(tt.sub(i - 1), tt.sub(i), rel));
                        know(tt.sub(i), newRelative(tt.sub(i), tt.sub(i - 1), -rel));
                    }
                    last = subStart;


                }


            }

        }
    }


    void know(Term term, @Nullable Event event) {

        if (event != null) {
            SortedSet<Event> l = constraints.computeIfAbsent(term, (t) -> new TreeSet<>());
            l.add(event);
        }

        switch (term.op()) {
//            case NEG:
//                Term u = term.unneg();
//                SortedSet<Event> m = constraints.computeIfAbsent(u, (t) -> new TreeSet<>());
//                m.add(relative(u, term, 0, term.dtRange()));
//                break;
            case CONJ:
                int tdt = term.dt();
                if (tdt != XTERNAL) {
                    //add the known timing of the conj's events
                    TermContainer ss = term.subterms();
                    int sss = ss.size();
                    for (int i = 0; i < sss; i++) {
                        Term sub = ss.sub(i);
                        int w = term.subtermTime(sub);
                        know(sub, relative(sub, term, w, w + sub.dtRange()));
                    }
                }
                break;
        }

    }

    public boolean fullyEternal() {
        return !events().anyMatch(x -> x instanceof AbsoluteEvent && ((AbsoluteEvent) x).start != ETERNAL);
    }

    public Stream<? extends Event> events() {
        return constraints.values().stream().flatMap(Collection::stream);
    }

    public Event solve(final Term x, Map<Term, Time> trail) {

        //System.out.println("solve " + target + "\t" + trail);

        if (trail.containsKey(x)) {
            Time xs = trail.get(x);
            if (xs != null)
                return solution(x, xs);
            else
                return null; //cyclic
        }


        Time ph;
        if (fullyEternal() && empty(trail) && x.op() != IMPL && x.dt() != XTERNAL) {
            ph = Time.the(ETERNAL, 0); //glue
            knowTerm(x, ETERNAL); //everything will be relative to this, in eternity
        } else {
            ph = null;
        }
        trail.put(x, ph); //placeholder


        SortedSet<Event> cc = constraints.get(x);
        if (cc != null) {
            for (Event e : cc) {

                //System.out.println(x + " " + i + "\t" + trail + "\t" + e);

                Time xt = e.start(trail);
                if (xt != null) {
                    trail.put(x, xt);
                    return e;
                }
            }

        }

        if (x.size() > 0) {
            Event e = solveComponents(x, trail);

            if (e != null) {
                Time xs = e.start(trail);
                if (xs != null) {
                    trail.put(x, xs); //assign
                    return e;
                }
            }
        }

        trail.remove(x); //remove placeholder

//        //update constraints, in case they were changed above
//        cc = constraints(x);
//        if (cc!=null)
//            return cc.get(0);
//        else
        return null;
    }

    static boolean empty(Map<Term, Time> trail) {
        return trail.isEmpty() || !trail.values().stream().anyMatch(Objects::nonNull);
    }

    private Event solveComponents(Term target, Map<Term, Time> trail) {

        Op o = target.op();

        if (o == NEG) {
            Event ss = solve(target.unneg(), trail);
            if (ss != null)
                return ss.neg();
            else
                return null;
        } else if (o.temporal) {
            if (target.dt() != XTERNAL) {
                //TODO verify that the provided subterm timing is correct.
                // if so, return the input as-is
                // if not, return null
                if (target.op() == CONJ && target.size() == 2) {
                    Term a = target.sub(0);
                    Event ae = solve(a, trail);
                    if (ae != null) {
                        Term b = target.sub(1);
                        Event be = solve(b, trail);
                        if (be != null) {
                            Time at = ae.start(trail);
                            if (at != null) {
                                Time bt = be.start(trail);
                                if (bt != null) {
                                    return solveConj(a, at, b, bt);
                                }
                            }
                        }
                    }
                }
            } else /*if (target.dt() == XTERNAL)*/ {
                TermContainer tt = target.subterms();

                int tts = tt.size();
                assert (tts > 1);

                if (tts == 2) {


                    boolean dir = true; //forward
                    Term t0 = tt.sub(0);
                    Term t1 = tt.sub(1);

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
                                if (o == CONJ /*&& (a.op() == CONJ || b.op() == CONJ)*/) {
                                    //conjunction merge, since the results could overlap
                                    //either a or b, or both are conjunctions. and the result will be conjunction

                                    Event e = solveConj(a, at, b, bt);
                                    if (e != null)
                                        return e;

                                } else {

                                    Event e = solveTemporal(o, a, at, b, bt);
                                    if (e != null)
                                        return e;

                                }
                            } catch (UnsupportedOperationException e) {
                                logger.warn("temporalization solution: {}", e.getMessage());
                                return null; //TODO
                            }
                        }
                    }
                } else /* 3 or more, so dt=DTERNAL or dt=0 */ {
                    assert (tts > 2);

                /* HACK quick test for the exact appearance of temporalized form present in constraints */

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


                    Event s0 = solve(tt.sub(0), trail);
                    if (s0 != null) {
                        Event s1 = solve(tt.sub(1), trail);
                        if (s1 != null) {
                            int dt = dt(s0, s1, trail);
                            if (dt == 0 || dt == DTERNAL) {
                                return new TimeEvent(this, o.the(dt, tt.toArray()), s0.start(trail));
                            } else {
                                return null; //invalid
                            }
                        }
                    }

                }
            }
        }


        /** compute the temporal intersection of all involved terms. if they are coherent, then
         * create the solved term as-is (since it will not contain any XTERNAL) with the appropriate
         * temporal bounds. */
        if (o.statement)

        {
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
                    return new SolutionEvent(this, target, r.start(trail).abs(), r.end(trail).abs());

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


    private Event solveConj(Term a, Time at, Term b, Time bt) {
        long ata = at.abs();
        long bta = bt.abs();

        if (ata == ETERNAL && bta == ETERNAL) {
            ata = at.offset; //TODO maybe apply shift, and also needs to affect 'start'
            bta = bt.offset;// + a.dtRange();
        } else if (ata == ETERNAL ^ bta == ETERNAL) {
            return null; //one is eternal the other isn't
        }


        Term newTerm = Op.conjMerge(a, ata, b, bta);
        if (newTerm instanceof Bool) //failed to create conj
            return null;

        Time early = ata < bta ? at : bt;
        return new TimeEvent(this, newTerm, early);
    }

    private Event solveStatement(Term target, Map<Term, Time> trail, Event ra, Event rb) {
        long[] ii = intersect(ra, rb, trail);
        if (ii != null) {
            //overlap or adjacent
            return new SolutionEvent(this, target, ii[0], ii[1]);
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
                                return new SolutionEvent(this, target, occ);
                            }
                        }
                    }
                }
            }

        }
        return null;
    }

    private Event solveTemporal(Op o, Term a, Time at, Term b, Time bt) {

        assert (o != CONJ && o.temporal);

        int dt = dt(at, bt);
        if (dt != XTERNAL) {

            int innerRange = a.dtRange(); //only A, not B (because the end of A points to the start of B)
//            if (dt > 0) {
            dt -= innerRange;
//            } else if (dt < 0) {
//                dt += innerRange;
//            }

            if (dt != 0 && Math.abs(dt) < dur)
                dt = 0; //perceived as simultaneous within duration


            Term newTerm = o.the(dt, a, b);

            Time start = at;
            if (o == CONJ && start.abs() != ETERNAL && dt != DTERNAL) {
                long bStart = bt.abs();
                if (bStart != ETERNAL) {
                    if (bStart < start.abs())
                        start = bt;
                }

            }

            return new TimeEvent(this, newTerm, start);
        }
        return null;

    }


    @Override
    public String toString() {
        return constraints.values().stream().flatMap(Collection::stream).map(Object::toString).collect(Collectors.joining(","));
    }


}
