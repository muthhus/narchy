package nars.derive.time;

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
public class Temporalize implements ITemporalize {

    final static Logger logger = LoggerFactory.getLogger(Temporalize.class);

    /**
     * constraint graph
     */
    public final Map<Term, FasterList<Event>> constraints = new HashMap<>();
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

    /**
     * heuristic for ranking temporalization strategies
     */
    @Override public float score(Term x) {
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


    public SolutionEvent solution(Term term, Time start) {
        long st = start.abs();
        long et;
        if (st == ETERNAL) et = ETERNAL;
        else et = term.op() == CONJ ? st + term.dtRange() : st;

        return new SolutionEvent(this, term, st, et);
    }

    static String timeStr(int when) {
        return when != DTERNAL ? (when != XTERNAL ? Integer.toString(when) : "?") : "DTE";
    }

    public RelativeEvent newRelative(Term term, Term relativeTo, int start) {
        return new RelativeEvent(this,  term, relativeTo, start);
    }
    RelativeEvent relative(Term term, Term relativeTo, int start, int end) {
        return new RelativeEvent(this,  term, relativeTo, start, end);
    }


    protected void print() {

        constraints.forEach((k, v) -> {
            for (Event vv : v)
                System.out.println(k + " " + vv);
        });

        System.out.println();
    }


    @Override public void know(Task task, Subst d, boolean rooted) {
        Term taskTerm = task.term();

        AbsoluteEvent root =
                (rooted) ?
                        absolute(taskTerm, task.start(), task.end())
                        :
                        null;

        know(taskTerm, d, root);
    }

    @Override public void know(Term term, Subst d, @Nullable AbsoluteEvent root) {
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
        know(absolute(term, from, to), term,
                0, (int) (to - from)
        );
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

        if (parent != null) {
            add(parent, term, start, end);
        }


        Op o = term.op();
        if (o.temporal) {
            int dt = term.dt();

            if (dt == XTERNAL) {

                //TODO UNKNOWN TO SOLVE FOR
                //throw new RuntimeException("no unknowns may be added during this phase");

            } else {
                boolean reverse;
                if (dt == DTERNAL) {
                    dt = 0;
                    reverse = false;
                } else if (dt >= 0) {
                    reverse = false;
                } else {
                    reverse = true;
                }

                TermContainer tt = term.subterms();

                int l = tt.size();

                if (!reverse) {

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
                        know((!term.op().statement) ? parent : null, st, subStart, subEnd); //parent = null;

                        t = subEnd; //the duration of the event


                        if (i > 0) {
                            //crosslink adjacent subterms
                            int rel = subStart - last;
                            add(tt.sub(i - 1), newRelative(tt.sub(i - 1), tt.sub(i), rel));
                            add(tt.sub(i), newRelative(tt.sub(i), tt.sub(i - 1), -rel));
                        }
                        last = subStart;


                    }
                } else {

                    int t = end;
                    int last = DTERNAL;

                    //System.out.println(tt + " presubs " + t + "..reverse=" + reverse);
                    for (int i = l - 1; (i >= 0); i--) {

                        Term st = tt.sub(i);

                        if (i < l - 1)
                            t -= dt;

                        int sdt = st.dtRange();
                        int subEnd = t, subStart = t - sdt;

                        //                  System.out.println(parent + "\t" + st + " sub(" + i + ") " + subStart + ".." + subEnd);

                        //the event is atomic, so forget the parent in computing the subterm relations (which is in IMPL only)
                        know(/*(!term.op().statement) ?*/ parent /*: null*/, st, subStart, subEnd); //parent = null;


                        t = subStart; //the duration of the event


                        if (i < l - 1) {
                            //crosslink adjacent subterms
                            int rel = last - subEnd;
                            add(tt.sub(i), newRelative(tt.sub(i), tt.sub(i + 1), -rel));
                            add(tt.sub(i + 1), newRelative(tt.sub(i + 1), tt.sub(i), +rel));
                        }
                        last = subStart;


                    }
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


    void add(@NotNull Event root, Term term, int start, int end) {
        Event event;

        if (term.equals(root.term)) {
            event = root;
        } else {
            Time occ = root.start(null);
            assert (occ.base != XTERNAL);
            event = (occ.base != ETERNAL ?
                    absolute(term, occ.abs() + start, occ.abs() + end) :
                    relative(term, root.term, start, end)
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
            m.add(relative(u, term, 0, term.dtRange()));
            if (m.size() > 1)
                m.sortThis();
        }

        if (l.size() > 1)
            l.sortThis();
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
                            return new SolutionEvent(this, o.the(dt, tt.toArray()), s0.start(trail).abs());
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
        return new SolutionEvent(this, newTerm, start);
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

            return new SolutionEvent(this, newTerm, start);
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
