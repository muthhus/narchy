package nars.derive;

import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.derive.Temporalize.Time.Unknown;
import static nars.time.Tense.*;

/**
 * set missing temporal relations in a derivation using constraint solver
 *
 * @see http://choco-solver.readthedocs.io/en/latest/1_overview.html#directly
 */
public class Temporalize {
    /**
     * constraint graph (lazily constructed)
     */
    final Map<Term, FasterList<Event>> constraints = new HashMap();

    abstract static class Event implements Comparable<Event> {

        public final Term term;

        Event(Term term) {
            this.term = term;
        }

        abstract public Time start(HashMap<Term,Time> trail);

        abstract public Time end(HashMap<Term,Time> trail);

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

        public Event neg(boolean isNeg) {
            return isNeg ? neg() : this;
        }

        @Override
        public int compareTo(@NotNull Temporalize.Event o) {
            if (this == o) return 0;
            else {
                if (getClass() == o.getClass()) {
                    //same class, rank by term volume
                    if (this instanceof RelativeEvent) {
                        Term x = ((RelativeEvent)this).rel.term();
                        Term y = ((RelativeEvent)o).rel.term();
                        int vc = Integer.compare(x.volume(), y.volume());
                        if (vc == 0) {
                            return x.compareTo(y);
                        } else {
                            return vc;
                        }
                    }
                }

                if (this instanceof AbsoluteEvent)
                    return -1;
                else
                    return +1;

            }
        }
    }

    static int dt(Event a, Event b) {
        if (a instanceof AbsoluteEvent && b instanceof AbsoluteEvent) {
            return dt(a.start(null), b.end(null));
        } else if (a instanceof RelativeEvent && b instanceof RelativeEvent) {
            RelativeEvent ra = (RelativeEvent) a;
            RelativeEvent rb = (RelativeEvent) b;
            if (ra.rel.equals(rb.rel)) {
                //easy case
                return rb.end - ra.start;
            } else {
                //needs solved in the constraint graph
            }
        }

        return XTERNAL;
    }

    static int dt(Time a, Time b) {

        assert(a.base!=XTERNAL); assert(b.base!=XTERNAL);

        if (a.base == ETERNAL && b.base == ETERNAL) {
            return b.offset - a.offset; //relative offsets within an eternal context
        } else if (a.base != ETERNAL && b.base != ETERNAL) {
            return (int) (b.abs() - a.abs()); //TODO check for numeric precision loss
        } else {
            throw new UnsupportedOperationException("?"); //maybe just return DTERNAL
        }
    }

    static class AbsoluteEvent extends Event {

        public final long start, end;

        AbsoluteEvent(Term term, long start, long end) {
            super(term);

            assert ((start == ETERNAL && end == ETERNAL) || (start != ETERNAL && end != ETERNAL)) :
                    "invalid semi-eternalization: " + start + " " + end;

            if (start <= end) {
                this.start = start;
                this.end = end;
            } else {
                this.start = end;
                this.end = start;
            }
        }

        @Override
        public Event neg() {
            return new AbsoluteEvent($.neg(term), start, end);
        }

        @Override
        public Time start(HashMap<Term,Time> ignored) {
            return Time.the(start, XTERNAL);
        }

        @Override
        public Time end(HashMap<Term,Time> ignored) {
            return Time.the(end, XTERNAL);
        }

        @Override
        public String toString() {
            if (start != ETERNAL) {
                if (start != end)
                    return term + ("@[" + timeStr(start) + ".." + timeStr(end)) + "]";
                else
                    return term + "@" + timeStr(start);
            } else
                return term + "@ETE";
        }

    }

    public class SolutionEvent extends AbsoluteEvent {

        SolutionEvent(Term term, long start) {
            super(term, start, start + term.dtRange());
        }

        SolutionEvent(Term unknown) {
            super(unknown, XTERNAL, XTERNAL);
        }
    }

    /** used for preserving an offset within an eternal context */
    static class Time {

        public static Time Unknown = new Time(ETERNAL, XTERNAL);
        public final long base;
        public final int offset;

        @Override
        public String toString() {
            return str(base) + "|" + str(offset);
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

        static Time the(long when) {
            return the(when, 0);
        }

        static Time the(long base, int offset) {
            if (base == ETERNAL && offset == XTERNAL)
                return Unknown;
            else {

                if (base != ETERNAL && offset!=DTERNAL && offset!=XTERNAL)
                    return new Time(base + offset, 0); //direct absolute
                else
                    return new Time(base, offset);
            }
        }

        private Time(long base, int offset) {
            this.base = base;
            this.offset = offset;
        }

        public Time add(int offset) {

            assert(this.offset!=DTERNAL && offset!=DTERNAL);

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
            return base;
        }
    }

    class RelativeEvent extends Event {
        private final Term rel;
        private final int start, end;

        public RelativeEvent(Term term, Term relativeTo, int start) {
            this(term, relativeTo, start, start + term.dtRange());
        }

        public RelativeEvent(Term term, Term relativeTo, int start, int end) {
            super(term);
            assert (!term.equals(relativeTo));
            this.rel = relativeTo;
            this.start = start;
            this.end = end;
        }

        @Override
        public Event neg() {
            return new RelativeEvent($.neg(term), rel, start, end);
        }

        @Override
        public Time start(HashMap<Term,Time> trail) {
            return resolve(this.start, trail);
        }

        @Override
        public Time end(HashMap<Term,Time> trail) {
            return resolve(this.end, trail);
        }

        protected Time resolve(int offset, HashMap<Term,Time> trail) {

            Time rt = solveTime(rel, trail);
            if (rt!=null) {
                return rt.add(offset);
            } else {
                return null;
            }
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

//    void add(Event v, MutableValueGraph<Event, Integer> g) {
//
////        if (g instanceof RelativeEvent) {
////            add((((RelativeEvent) v).rel), g); //add relation first
////        }
//
//        if (!g.addNode(v))
//            return; //already added
//
//        Term vt = v.term;
//        if (vt.op().temporal) {
//            int d = vt.dt();
//            if (d != XTERNAL) {
//                int s = vt.size();
//                if (s == 2) {
//                    Term a = vt.sub(0);
//                    Term b = vt.sub(1);
//                    if (!a.equals(b)) {
//                        Event ae = events.get(a);
//                        if (ae != null) {
//                            Event be = events.get(b);
//                            if (be != null) {
//                                int at = vt.subtermTime(a);
//                                int bt = vt.subtermTime(b);
//
//                                Event from, to;
//                                if (ae.term.compareTo(be.term) < 0) {
//                                    from = ae; to = be;
//                                } else {
//                                    from = be; to = ae;
//                                }
//
//                                if (null == g.edgeValueOrDefault(from, to, null)) {
//                                    int delta;
//                                    if (from == ae) {
//                                        delta = bt - at;
//                                    } else {
//                                        delta = at - bt;
//                                    }
//                                    g.putEdgeValue(from, to, delta);
//                                }
//                            }
//                        }
//                    }
//
//                } else if (s > 2) {
//                    //connect all the edges
//                }
//            }
//        }
//
//
//    }


    @Nullable
    public static Event solve(@NotNull Derivation d, Term pattern, HashMap<Term,Time> times) {

        /*
        unknowns to solve otherwise the result is impossible:
            - derived task start time
            - derived task end time
            - dt intervals for any XTERNAL appearing in the input term
        knowns:
            - for each task and optional belief in the derived premise:
                - start/end time of the task
                - start/end time of any contained events
            - possible relations between events referred to in the conclusion that
                appear in the premise.  this may be partial due to variable introduction
                and other reductions. an attempt can be made to back-solve the result.
                if that fails, a heuristic could decide the match. in the worst case,
                the derivation will not be temporalizable and this method returns null.
        */
        Task task = d.task;
        Task belief = d.belief;


        Temporalize model = new Temporalize();

        model.know(task, d);
        if (belief != null)
            model.know(belief, d);


        return model.solve(pattern, times);
    }

    public void know(Task task, Derivation d) {
        assert (task.end() == task.start() + task.dtRange());
        Term taskTerm = task.term();
        AbsoluteEvent root = new AbsoluteEvent(taskTerm, task.start(), task.end());
        know(root, taskTerm, 0, taskTerm.dtRange());

        Term t2 = d.transform(taskTerm);
        if (!t2.equals(taskTerm)) {
            know(root, t2, 0, t2.dtRange());
        }
    }

    /**
     * convenience method for testing: assumes start offset of zero, and dtRange taken from term
     */
    Temporalize knowTerm(Term term, long when) {
        know(new AbsoluteEvent(term, when, when != ETERNAL ? when + term.dtRange() : ETERNAL), term, 0, term.dtRange());
        return this;
    }


    /**
     * recursively calculates the start and end time of all contained events within a term
     *
     * @param occ superterm occurrence, may be ETERNAL
     */
    void know(@Nullable Event parent, Term term, int start, int end) {

        //TODO support multiple but different occurrences  of the same event term within the same supercompound
        if (parent.term != term) {
            List<Event> exist = constraints.get(term);
            if (exist != null)
                return;
        }

        Event event = add(parent, term, start, end);

        if (term instanceof Compound) {
            Compound c = (Compound) term;
            Op o = c.op();
            if (o.temporal) {
                int dt = c.dt();

                if (dt == XTERNAL) {

                    //TODO UNKNOWN TO SOLVE FOR
                    throw new RuntimeException("no unknowns may be added during this phase");

                } else {
                    if (dt == DTERNAL)
                        dt = 0;

                    TermContainer tt = c.subterms();
                    if (dt < 0 && o.commutative) {
                        dt = -dt;
                        tt = c.reverse();
                    }

                    int t = start;

                    int l = tt.size();

                    //System.out.println(tt + " presubs " + t + "..reverse=" + reverse);
                    int lastSubStart = DTERNAL;
                    for (int i = 0; (i < l); i++) {

                        Term st = tt.sub(i);
                        int sdt = st.dtRange();

                        int subStart = t;
                        int subEnd = t + sdt;
                        //System.out.println("\t" + st + " sub(" + i + ") " + subStart + ".." + subEnd);
                        know(parent, st, subStart, subEnd);

                        t = subEnd; //the duration of the event

                        if (i < l - 1)
                            t += dt; //the dt offset (doesnt apply to the first term which is early/left-aligned)

                        if (i > 0) {
                            //crosslink adjacent subterms
                            add(tt.sub(i-1), new RelativeEvent(tt.sub(i-1), tt.sub(i), lastSubStart - subStart));
                            add(tt.sub(i), new RelativeEvent(tt.sub(i), tt.sub(i-1), subStart - lastSubStart));
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

    }

    Event add(@Nullable Temporalize.Event root, Term term, int start, int end) {
        Event event;
        if (term.equals(root.term)) {
            event = root;
        } else {
            Time occ = root.start(null);
            assert(occ.base!=XTERNAL);
            event = (occ.base != ETERNAL ?
                new AbsoluteEvent(term, occ.abs() + start, occ.abs() + end) :
                new RelativeEvent(term, root.term, start, end)
            );
        }
        add(term, event);
        return event;
    }



    void add(Term term, Event event) {
        FasterList<Event> l = constraints.computeIfAbsent(term, (t) -> new FasterList<>());
        if (l.isEmpty()) {
            l.add(event);
        } else {
            l.add(0, event);
            l.sortThis();
        }
    }

    Event solve(Term target) {
        return solve(target, new HashMap());
    }

    Event solve(Term target, HashMap<Term, Time> times) {
        boolean isNeg = target.op() == NEG;
        if (isNeg)
            target = target.unneg();


        Event known = solveGraph(target, times);
        if (known != null)
            return known.neg(isNeg);

        if (target instanceof Compound) {

            Compound c = (Compound) target;
            Op o = c.op();
            if (o.temporal) {
                int dt = c.dt();

                if (dt == XTERNAL) {

                    TermContainer tt = c.subterms();
                    assert (tt.size() == 2);

                    Term a = tt.sub(0);
                    Time at = solveTime(a, times);

                    if (at!=null) {

                        Term b = tt.sub(1);
                        Time bt = solveTime(b, times);

                        if (bt != null) {

                            int sd = dt(at, bt);
                            if (sd != XTERNAL) {
                                //direct solution found
                                return new SolutionEvent(
                                        o.the(sd, new Term[]{a, b}),
                                        o == CONJ ? Math.min(at.abs(), bt.abs()) : at.abs()
                                ).neg(isNeg);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Nullable private Time solveTime(Term target, HashMap<Term,Time> trail) {

        Time existing = trail.get(target);
        if (existing!=null)
            return existing;

        trail.put(target, Unknown); //placeholder to prevent infinite loop

        List<Event> ea = constraints.get(target);
        if (ea != null) {

            for (Event x : ea) {
                Time xs = x.start(trail);
                if (xs == null)
                    continue;

                //System.out.println(target + " @ " + xs + " " + trail);
                trail.put(target, xs);
                return xs;
            }


        }

        return null;
    }


    private Event solveGraph(Term target, HashMap<Term,Time> trail) {
        //compute indirect solution from constraint graph
        //trail.add(target);

        List<Event> ea = constraints.get(target);
        if (ea != null) {

            for (Event x : ea) {
                Time xs = x.start(trail);
                if (xs!= Unknown) {
                    //System.out.println(target + " @ " + xs + " " + trail);
                    return x;
                }
            }


        }

        return null;
    }

    @Override
    public String toString() {
        return constraints.values().stream().flatMap(Collection::stream).map(Object::toString).collect(Collectors.joining(","));
    }


    static String timeStr(long when) {
        return when != ETERNAL ? Long.toString(when) : "ETE";
    }

    static String timeStr(int when) {
        return when != DTERNAL ? (when != XTERNAL ? Integer.toString(when) : "?") : "DTE";
    }
}
