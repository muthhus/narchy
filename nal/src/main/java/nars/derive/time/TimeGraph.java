package nars.derive.time;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jcog.Util;
import jcog.data.graph.hgraph.Edge;
import jcog.data.graph.hgraph.HashGraph;
import jcog.data.graph.hgraph.Node;
import jcog.data.graph.hgraph.Search;
import jcog.list.FasterList;
import nars.$;
import nars.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
import static nars.derive.time.TimeGraph.TimeSpan.TS_ZERO;
import static nars.time.Tense.*;

/**
 * represents a multigraph of events and their relationships
 * calculates unknown times by choosing from the possible
 * pathfinding results.
 * <p>
 * it can be used in various contexts:
 * a) the tasks involved in a derivation
 * b) as a general purpose temporal index, ie. as a meta-layer
 * attached to one or more concept belief tables
 * <p>
 * DTERNAL relationships can be maintained separate
 * from +0.
 */
public class TimeGraph extends HashGraph<TimeGraph.Event, TimeGraph.TimeSpan> {

    private boolean allowSelfLoops = true;

    public static class TimeSpan {
        public final long dt;
        //public final float weight;

        public final static TimeSpan TS_ZERO = new TimeSpan(0);
        //        public final static TimeSpan TS_POS_ONE = new TimeSpan(+1);
//        public final static TimeSpan TS_NEG_ONE = new TimeSpan(-1);
        public final static TimeSpan TS_ETERNAL = new TimeSpan(ETERNAL);

        public static TimeSpan the(long dt) {
            assert (dt != TIMELESS);

            if (dt == 0) {
                return TS_ZERO;
            } else if (dt == ETERNAL) {
                return TS_ETERNAL;
            } else {
                return new TimeSpan(dt);
            }
        }

        private TimeSpan(long dt) {
            this.dt = dt;
            //this.weight = weight;
            //this.hash = Long.hashCode(dt);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(dt);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || ((obj instanceof TimeSpan && dt == ((TimeSpan) obj).dt));
        }

        @Override
        public String toString() {
            return (dt == ETERNAL ? "~" : (dt >= 0 ? ("+" + dt) : ("-" + (-dt))));
            //+ (weight != 1 ? "x" + n2(weight) : "");
        }
    }

    /**
     * index by term
     */
    public final Multimap<Term, Event> byTerm = HashMultimap.create();


    public TimeGraph() {

    }

    public Event know(Term t) {
        return know(t, TIMELESS);
    }

    public Event know(Termed t, long start) {
        assert (!(t instanceof Bool));

        Event e = event(t, start);

        return add(e).id;
    }


    public TimeGraph.Event event(Termed t, long start) {
        Event e;
        Term tt = t.term();


        if (t instanceof Task) {
            Task ta = ((Task) t);
            e = new AbsoluteInterval(ta.term(), ta.start(), ta.end());
        } else {
            if (tt.op() == CONJ && tt.dtRange() != 0) {
                e = new Sequence(t.term(), start);
            } else if (start == TIMELESS) {
                e = new Relative(t.term());
            } else {
                e = new Absolute(t.term(), start);
            }

        }
        return e;
    }

    public void link(Event x, long dt, Event y) {

        if (!allowSelfLoops && (x == y || ((x.id.equals(y.id)) && (x.start() == y.start() || x.end() == y.end()))))
            return; //loop

        boolean swap = false;
//        if (dt == ETERNAL || dt == TIMELESS || dt == 0) {
        //lexical order
        int vc = Integer.compare(x.id.volume(), y.id.volume());
        if (vc == 0) {

            if (x.hashCode() > y.hashCode()) { //TODO write real comparator
                swap = true;
            }
        } else if (vc > 0) {
            swap = true;
        }

        if (swap) {
            if (dt != ETERNAL && dt != TIMELESS && dt != 0) {
                dt = -dt;
            }
            Event z = x;
            x = y;
            y = z;
        }

        link(x, TimeSpan.the(dt), y);
    }

    @Override
    protected void onAdd(Node<Event, TimeSpan> x) {
        Event event = x.id;
        Term eventTerm = event.id;

        byTerm.put(eventTerm, event);
        Term tRoot = eventTerm.root();
        if (!tRoot.equals(eventTerm))
            byTerm.put(tRoot, event);

        switch (eventTerm.op()) {
            case NEG: {
                link(know(eventTerm.unneg()), 0, event); //lower priority?
                break;
            }
            case IMPL: {

                int dt = eventTerm.dt();
                Term subj = eventTerm.sub(0);
                Term pred = eventTerm.sub(1);
                Event se = know(subj);
                Event pe = know(pred);
                if (dt != XTERNAL) {

                    int st = subj.dtRange();

                    link(se, dt == DTERNAL ? ETERNAL : (dt + st), pe);

//                    if (subj.hasAny(CONJ)) {
//                        subj.eventsWhile((w, y) -> {
//                            link(know(y), dt+st+-w, pe);
//                            return true;
//                        }, 0, false, false, 0);
//                    }
//                    if (pred.hasAny(CONJ)) {
//                        pred.eventsWhile((w, y) -> {
//                            link(se, dt+st+w, know(y));
//                            return true;
//                        }, 0, false, false, 0);
//
//                    }

                }

                //link(se, 0, event); //WEAK

                break;
            }
            case CONJ:
                TermContainer tt = eventTerm.subterms();
                long et = event.start();

                int eventDT = eventTerm.dt();

                int s = tt.subs();
//                if (et == TIMELESS) {
//                    //chain the sibling subevents
//                    if (s == 2) {
//                        Term se0 = tt.sub(0);
//                        Event e0 = know(se0);
//                        Term se1 = tt.sub(1);
//                        Event e1 = know(se1);
//                        int dt;
//                        Event earliest;
//                        if (eventDT == DTERNAL) {
//                            dt = DTERNAL;
//                            earliest = e0; //just use the first by default
//                        } else {
//                            long t0 = eventTerm.subTime(se0);
//                            long t1 = eventTerm.subTime(se1);
//                            long ddt = (int) (t1 - t0);
//                            assert (ddt < Integer.MAX_VALUE);
//                            dt = (int) ddt;
//                            earliest = t0 < t1 ? e0 : e1;
//                        }
//                        link(e0, dt, e1);
//                        link(earliest, 0, event);
//
//                    } else {
//                        throw new TODO();
//                    }
//
//                } else
                if (eventDT == DTERNAL) {
                    /* //chain the children subevents events
                    for (int i = 0; i < s; i++) {
                        Term rr = tt.sub(i);
                        Event subEvent = know(rr);
                        if (eventDT == DTERNAL) {
                            //link(event, ETERNAL, subEvent);
                        } else {
                            long rt = eventTerm.subTime(rr);
                            link(event, rt, subEvent);
                        }
                    }*/

                } else {
                    //locate the events and sub-events absolutely
                    eventTerm.eventsWhile((w, y) -> {
                        if (eventDT != XTERNAL) {
                            link(event, w,
                                    et != TIMELESS ?
                                            know(y, w + et) :
                                            know(y)
                            );
                        } else {
                            know(y);
                        }

                        return true;
                    }, 0, true, false, true, 0);
                }

                break;
        }

    }

//    public void know(Term t, long start, float w) {
//        if (start != ETERNAL) {
//            long end = t.dtRange() + start;
//            if (end == start) {
//                add(new TimeProblem.Event(t, start));
//            } else {
//                link(new TimeProblem.Event(t, start), r(end - start, w), new TimeProblem.Event(t, end));
//            }
//        }
//
//    }

    final static int MAX_SUBTERM_SOLUTIONS = 2;

    void solveDT(Term x, Map<Term, LongSet> absolute, Predicate<Event> each) {
        assert (x.dt() == XTERNAL);

        TermContainer xx = x.subterms();
        for (Event r : byTerm.get(x.root())) {
            if (r.absolute()) {
                Term rt = r.id;
                if (rt.subterms().equals(xx)) {
                    if (!each.test(r))
                        return; //done
                }
            }

        }

        int subs = x.subs();
        if (subs == 2) {
            Term a = xx.sub(0);
            Term b = xx.sub(1);

            Collection<Event> aTerms = byTerm.get(a);
            Collection<Event> bTerms = !b.equals(a) ? byTerm.get(b) : List.of();
            int ns = aTerms.size() + bTerms.size();
            List<Event> sources = $.newArrayList(ns);
            sources.addAll(aTerms);
            sources.addAll(bTerms);
            Collections.shuffle(sources, random()); //TODO use (possibly biased) roulette sampling

            boolean repeat = a.unneg().equals(b.unneg()); //if true, then we must be careful when trying this in a commutive-like result which would collapse the two terms

            searchDepthFirst(sources, true, true,
                    new DTSolver(a, b, (long start, long ddt) -> {
                        assert (ddt < Integer.MAX_VALUE);
                        int dt = (int) ddt;
                        if (repeat && (dt == 0 || dt == DTERNAL)) {
                            //this will result in collapsing the term to just one, with no temporal separation
                            //this is like an accidental shortcut it has tried - but it is not helpful information for us
                            //try again
                            return true;
                        }
                        Term y = x.dt(dt);
                        if (!(y instanceof Bool)) {
                            return each.test(start != TIMELESS ?
                                    event(y, start) : new Unsolved(y, absolute));
                        } else {
                            return true;
                        }
                    }));
        } else {

        }


//        if (x.subs() == 2) {
//            Term a = xx.sub(0);
//            Set<Event<Term>> ae = absolutes(a);
//            if (!ae.isEmpty()) {
//                Term b = xx.sub(1);
//                Set<Event<Term>> be = absolutes(b);
//                if (!be.isEmpty()) {
//                    //cartesian product of the two, maybe prioritized by least distance?
//                    LazyIterable<Pair<Event<Term>, Event<Term>>> matrix = Sets.cartesianProduct(ae, be);
//                    matrix.allSatisfy(ab -> {
//
//                        long bt = ab.getTwo().start();
//                        long at = ab.getOne().end();
//
//                        int dt;
//                        if (bt == ETERNAL || at == ETERNAL) {
//                            dt = DTERNAL;
//                        } else {
//                            long ddt = bt - at;
//                            assert (Math.abs(ddt) < Integer.MAX_VALUE);
//                            dt = (int) ddt;
//                        }
//
//                        Term tt = x.dt(dt);
//                        if (tt.op().conceptualizable)
//                            return each.test(tt);
//                        else
//                            return true;
//                    });
//                }
//            }
//        }
    }


    protected Random random() {
        return ThreadLocalRandom.current();
    }


//    public Set<Event<Term>> absolutes(Term a) {
//        UnifiedSet<Event<Term>> ae = new UnifiedSet();
//        solve(a, (e) -> {
//            if (e.start() != TIMELESS && ae.add(e))
//                return ae.size() < MAX_SUBTERM_SOLUTIONS;
//            else
//                return true;
//        });
//        return ae.isEmpty() ? Set.of() : ae;
//    }

    final static LongSet EMPTY_LONG_SET = LongSets.immutable.empty();

    public void solve(Term x, Predicate<Event> _each) {

        Set<Event> seen = new HashSet();

        Predicate<Event> each = (y) -> {
            if (seen.add(y)) {
                return _each.test(y);
            } else {
                return true; //filtered
            }
        };

        solveAll(x, e -> {

            //if (Param.DEBUG) assert (e.id.root().equals(x.root()));

            if (!each.test(e)) {
                return false;
            }

            /*if (e instanceof Absolute) {
                return ;
            } else */
            if (e instanceof Unsolved) {

                //intermediate dirty result
//                if (!each.test(e))
//                    return false;

                //then try to solve it, as if any results are progressive enhancement of the current state
                return solve((Unsolved) e, each);
            } else {
                assert (!(e instanceof Relative));
            }
            return true;
        });
    }


    /**
     * each should only receive Event or Unsolved instances, not Relative's
     */
    void solveAll(Term x, Predicate<Event> each) {

        //1. test for existing solutions to the exact term
        for (Event e : byTerm.get(x)) {
            if (e.absolute() && !each.test(e))
                return;
        }


        Map<Term, Term> xternals = new UnifiedMap();

        Map<Term, LongSet> absolute = new LinkedHashMap();

        x.temporalize(xs -> {
            int dt = xs.dt();
            if (dt == XTERNAL) {
                xternals.put(xs, xs);
            }
            return dt; //no change
        });

        final boolean clue = absolutes(absolute, x);


//        if (!clue[0])
//            return; //no temporal basis to compute further

        if (!xternals.isEmpty()) {
            //resolve any XTERNAL in subterms
            final boolean[] foundAny = {false};
            xternals.replaceAll((u, z) -> {
                final Term[] vv = new Term[1];
                solveDT(u, absolute, (v) -> {
                    Term w = v.id;
                    if (w.equals(u)) {
                        return true; //same as input, try again TODO limit here
                    } else {
                        vv[0] = w; //ignore the startTime component, although it might provide a clue here
                        return false; //just one for now
                    }
                });
                if (vv[0] != null) {
                    foundAny[0] = true;
                    return vv[0];
                } else {
                    return u;
                }
            });
            if (foundAny[0]) {
                x = x.replace(xternals);
                //assert (!(y.equals(x)));
                if (x instanceof Bool)
                    return;


//                    solvable(y, each); //recurse
//                else
//                    return;
            }
        }


        if (x.dt() == XTERNAL) {
            //solve any XTERNAL at top-level
            solveDT(x, absolute, (y) -> {
                if (!each.test(y))
                    return false;

                if (y instanceof Unsolved)
                    return solve((Unsolved) y, each);
                else
                    return true;
            });
        } else {
            solve(new Unsolved(x, absolute), each);
        }
    }

    protected boolean absolutes(Map<Term, LongSet> absolute, Term x) {

        if (absolute.putIfAbsent(x, EMPTY_LONG_SET) != null)
            return false; //already processed

        boolean clue[] = {false};

        x.eventsWhile((t, xx) -> {

            if (!x.equals(xx))
                clue[0] |= absolutes(absolute, xx);

            return true;

        }, 0, true, true, true, 0);

        if (x.op() == IMPL) {
            for (Term xx : x.subterms())
                clue[0] |= absolutes(absolute, xx);
        }

        Collection<Event> xe = byTerm.get(x);
        if (xe != null) {
            LongHashSet l = new LongHashSet();
            xe.forEach(e -> {
                if (e.absolute())
                    l.add(e.start());
            });

            if (!l.isEmpty()) {
                clue[0] = true;
                absolute.put(x, l.toImmutable());
            }
        }

        return clue[0];

    }


    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    private boolean solve(Unsolved u, Predicate<Event> each) {

        Map<Term, LongSet> a = u.absolute;

        Term targetTerm = u.id;

        return searchDepthFirst(
                u,
                true, true,
                new OccurrenceSolver(each));

    }


    abstract protected class TimeSolver extends Search<Event, TimeSpan> {

        protected TimeSolver() {
            super();
        }

        public Stream<Edge<Event, TimeSpan>> dynamicLink(Node<Event, TimeSpan> n, Predicate<Event> eventFilter) {


            Term nt = n.id.id;
            return byTerm.get(nt).stream()
                    .filter(x -> !x.equals(n.id))
                    .filter(eventFilter).map(e -> {
                                Node<Event, TimeSpan> that = TimeGraph.this.node(e);
                                return new Edge<>(n, that,
//                                outOrIn ? n : that,
//                                outOrIn ? that : n,
                                        TS_ZERO);
                            }
                    );
        }


//        public long startTime(FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {
//            BooleanObjectPair<Edge<Event<Term>, TimeSpan>> firstStep = path.get(0);
//            boolean outOrIn = firstStep.getOne();
//            return firstStep.getTwo().from(outOrIn).id.start();
//        }

        /**
         * computes the length of time spanned from start to the end of the given path
         */
        public long pathTime(FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {

            long t = 0;
            //compute relative path
            boolean dternal = false;
            for (int i = 0, pathSize = path.size(); i < pathSize; i++) {
                BooleanObjectPair<Edge<Event, TimeSpan>> r = path.get(i);
                Edge<Event, TimeSpan> event = r.getTwo();
                long spanDT = event.get().dt;

                if (spanDT == ETERNAL) {
                    //no change, crossed a DTERNAL step. this may signal something
                    dternal = true;
                } else if (spanDT != 0) {
                    long fromDT = event.from.id.dt();
                    t += (spanDT + fromDT) * (r.getOne() ? +1 : -1);
                }
            }

            return t;
        }

        /**
         * assuming the path starts with one of the end-points (a and b),
         * if the path ends at either one of them
         * this computes the dt to the other one,
         * and (if available) the occurence startTime of the path
         * returns (startTime, dt) if solved, null if dt can not be calculated.
         */
        @Nullable
        protected long[] pathDT(Node<Event, TimeSpan> n, Term a, Term b, FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {
            Term endTerm = n.id.id;
            boolean endA = a.equals(endTerm);
            boolean endB = b.equals(endTerm);

            if (endA || endB) {
                BooleanObjectPair<Edge<Event, TimeSpan>> startStep = path.getFirst();
                BooleanObjectPair<Edge<Event, TimeSpan>> endStep = path.getLast();
                Edge<Event, TimeSpan> startEdge = startStep.getTwo();
                Edge<Event, TimeSpan> endEdge = endStep.getTwo();

                Event startEvent = startEdge.from(startStep.getOne()).id;
                Event endEvent = endEdge.to(endStep.getOne()).id;
                Term startTerm = startEvent.id;

                if ((endA && startTerm.equals(b)) || (endB && startTerm.equals(a))) {


                    long startTime = startEvent.absolute() ? startEvent.start() : TIMELESS;
                    long endTime = endEvent.absolute() ? endEvent.start() : TIMELESS;

                    long dt;
                    if (startTime != TIMELESS && startTime != ETERNAL && endTime != TIMELESS && endTime != ETERNAL) {
                        //use the two endpoints and subtract the dt

                        dt = endTime - startTime;
                    } else {

                        //TODO more rigorous traversal of the dt chain
                        //compute from one end to the other, summing dt in the correct direction along the way
                        //special handling for encountered absolute terms and DTERNAL

                        dt = pathTime(path);
                    }

                    if (endA && dt != ETERNAL)
                        dt = -dt; //reverse

                    if (startTime != TIMELESS || endTime == TIMELESS) {
                        return new long[]{startTime, dt};
                    } else {
                        return new long[]{endTime - dt, dt};
                    }
                }
            }
            return null;
        }

    }

    protected class OccurrenceSolver extends TimeSolver {

        private final Predicate<Event> each;

        public OccurrenceSolver(Predicate<Event> each) {
            this.each = each;
        }

        @Override
        protected Stream<Edge<Event, TimeSpan>> edges(Node<Event, TimeSpan> n, boolean in, boolean out) {
            return Stream.concat(
                    super.edges(n, in, out),
                    dynamicLink(n, e -> true)
            );
        }


        @Override
        public boolean visit(Node<Event, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {
            if (path.isEmpty())
                return true;

            //System.out.println(path);

            Term current = n.id.id;

            if (n.id.absolute()) {

                long pathEndTime = n.id.start();
                BooleanObjectPair<Edge<Event, TimeSpan>> pathStart = path.get(0);
                Term pathStartTerm = pathStart.getTwo().from(pathStart.getOne()).id.id;
                if (!each.test(event(pathStartTerm,
                        pathEndTime == ETERNAL ? ETERNAL :
                                pathEndTime - pathTime(path))))
                    return false;
            }

            return true;
        }
    }

    protected class DTSolver extends TimeSolver {

        private final Term a, b;
        private final LongLongPredicate each;

        public DTSolver(Term a, Term b, LongLongPredicate each) {
            this.a = a;
            this.b = b;
            this.each = each;
        }

        @Override
        protected Stream<Edge<Event, TimeSpan>> edges(Node<Event, TimeSpan> n, boolean in, boolean out) {
            return Stream.concat(super.edges(n, in, out), dynamicLink(n, e -> true)).distinct();
        }


        @Override
        public boolean visit(Node<Event, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {

            if (path.isEmpty())
                return true;

            //System.out.println(path);

            long[] dt = pathDT(n, a, b, path);

            if (dt != null)
                if (!each.accept(dt[0], dt[1]))
                    return false;

            return true;
        }

    }

    /**
     * TODO not ready yet
     */
    protected class DTCommutiveSolver extends TimeSolver {
        private final Set<Term> targets;
        private final LongLongPredicate each;

        public DTCommutiveSolver(Set<Term> targets, LongLongPredicate each) {
            this.targets = targets;
            this.each = each;
        }

        @Override
        protected boolean visit(Node<Event, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event, TimeSpan>>> path) {
            if (path.isEmpty())
                return true;

            if (path.isEmpty())
                return true;

            //System.out.println(path);

            //long[] dt = pathDT(n, a, b, path);

//            if (dt!=null)
//                if (!each.accept(dt[0], dt[1]))
//                    return false;

            return true;
        }
    }

    //    @Override
//    public double cost(Event<T> a, Event<T> b) {
//        long at, bt;
//        if ((at = a.start()) == Long.MIN_VALUE || (bt = b.start()) == ETERNAL)
//            return Double.POSITIVE_INFINITY;
//        else
//            return Math.abs(at - bt);
//    }
//
//    /** TODO stream */
//    @Override public Iterable<Event<T>> next(Event<T> current) {
//        return ()->node(current).successors().iterator();
//    }

    public boolean link(Event before, TimeSpan e, Event after) {
        return edgeAdd(add(before), e, add(after));
    }

    /**
     * absolutely specified event
     */
    public abstract static class Event /*extends Find<ObjectLongPair<T>>*/ {

        public final Term id;
        private final int hash;

        protected Event(Term id, long start, long end) {
            this.id = id;
            this.hash = Util.hashCombine(id.hashCode(), Long.hashCode(start), Long.hashCode(end));
        }

        abstract public long start();

        abstract public long end();

        public long dt() {
            long s = start();
            if (s == ETERNAL || s == TIMELESS)
                return 0;
            else
                return end() - s;
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            Event e = (Event) obj;
            return start() == e.start() && id.equals(e.id) && end() == e.end();
        }

        //        public float pri() {
//            return 1f;
//        }


        @Override
        public final String toString() {
            long s = start();

            if (s == TIMELESS) {
                return id.toString();
            } else if (s == ETERNAL) {
                return id + "@ETE";
            } else {
                long e = end();
                return id + "@" + (s == e ? s : "[" + s + ".." + e + "]");
            }
        }

        public final boolean absolute() {
            return start() != TIMELESS;
        }
    }

    static class Absolute extends Event {
        protected final long start;

        private Absolute(Term t, long startAndEnd) {
            super(t, startAndEnd, startAndEnd);
            this.start = startAndEnd;
        }

        @Override
        public final long start() {
            return start;
        }

        @Override
        public long end() {
            return start;
        }
    }

    static class Sequence extends Absolute {

        private final int dt;

        private Sequence(Term t, long start) {
            super(t, start);
            assert (t.op() == CONJ);
            dt = t.dtRange();
        }

        @Override
        public long dt() {
            return dt;
        }

        @Override
        public long end() {
            return (start != ETERNAL && start != TIMELESS) ? start + id.dtRange() : start;
        }
    }

    public static class AbsoluteInterval extends Event {
        private final long start, end;

        public AbsoluteInterval(Term t, long start, long end) {
            super(t, start, end);
            assert (start != end);
            this.start = start;
            this.end = end;
        }

        @Override
        public final long start() {
            return start;
        }

        @Override
        public final long end() {
            return end;
        }
    }

    /**
     * floating, but potentially related to one or more absolute event
     */
    public static class Relative extends Event {

        public Relative(Term id) {
            super(id, TIMELESS, TIMELESS);
        }

        @Override
        public final long start() {
            return TIMELESS;
        }

        @Override
        public final long end() {
            return TIMELESS;
        }
    }

    /**
     * holds an attached solution-specific table of event times;
     * instances of Unsolved shouldnt be inserted
     */
    protected static class Unsolved extends Relative {
        public final Map<Term, LongSet> absolute;

        public Unsolved(Term x, Map<Term, LongSet> absolute) {
            super(x);
            this.absolute = absolute;
        }

        @Override
        public long dt() {
            throw new UnsupportedOperationException();
        }


        //        /* hack */
//        static final String ETERNAL_STRING = Long.toString(ETERNAL);
//
//        @Override
//        public String toString() {
//            return id + "?" +
//                    (absolute.toString().replace(ETERNAL_STRING, "ETE")); //HACK
//        }

    }


}

//        Stream<Event<Term>> src = a.entrySet().stream().flatMap(aa -> {
//            Term aaa = aa.getKey();
//            if (aa.getValue() == EMPTY_LONG_SET)
//                return byTerm.get(aaa).stream();
//            else
//                return Stream.empty();
//        });

//Relative target = (Relative) know(targetTerm);  //add the target to the graph

//find paths from the KNOWN to the UNKNOWN
//        List<Event<Term>> as = a.keySet().stream().flatMap(x -> byTerm.get(x).stream()).collect(toList());

//List<Event<Term>> as = Lists.newArrayList(
//byTerm.get(aa)
//Iterables.filter(byTerm.get(aa), Event::absolute));

