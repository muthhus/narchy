package nars.derive.time;

import astar.model.TimeProblem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jcog.Util;
import jcog.data.graph.hgraph.Edge;
import jcog.data.graph.hgraph.Node;
import jcog.data.graph.hgraph.Search;
import jcog.list.FasterList;
import nars.$;
import nars.Param;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.derive.time.TimeGraph.TimeSpan.TS_ZERO;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

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
public class TimeGraph extends TimeProblem<Term, TimeGraph.TimeSpan> {

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
    public final Multimap<Term, Event<Term>> byTerm = HashMultimap.create();


    public TimeGraph() {

    }

    public Event<Term> know(Term t) {
        assert (!(t instanceof Bool));
        Event e = new Relative(t);
        return (Event) add(e).id;
    }

    public Event know(Term t, long start) {
        return know(t, start, start);
    }

    public Event<Term> know(Term t, long start, long end) {
        assert (!(t instanceof Bool));
        assert (start != TIMELESS);
        Node<Event<Term>, TimeSpan> eventTimeSpanNode = add(
                start == end ?
                        new Absolute(t, start) :
                        new AbsoluteInterval(t, start, end)
        );
        return (Event) (eventTimeSpanNode.id);
    }

    public void link(Event<Term> x, long dt, Event<Term> y) {

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
    protected void onAdd(Node<Event<Term>, TimeSpan> x) {
        Event<Term> event = x.id;
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
                Event<Term> se = know(subj);
                Event<Term> pe = know(pred);
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

                //link(se, 0, event);

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
                if (eventDT == XTERNAL) {

                    //just know the subterms individually with no relation to each other or the parent
                    for (Term r : tt)
                        know(r);

                } else if (et == ETERNAL || et == TIMELESS || eventDT == DTERNAL) {
                    //chain the children subevents events
                    for (int i = 0; i < s; i++) {
                        Term rr = tt.sub(i);
                        Event subEvent = know(rr);
                        if (eventDT == DTERNAL) {
                            //link(event, ETERNAL, subEvent);
                        } else {
                            long rt = eventTerm.subTime(rr);
                            link(event, rt, subEvent);
                        }
                    }

                } else {
                    //locate the events and sub-events absolutely
                    eventTerm.eventsWhile((w, y) -> {
                        know(y, w);
                        return true;
                    }, et, false, false, 0);
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

    public void solveDT(Term x, Map<Term, LongSet> absolute, Predicate<Event<Term>> each) {
        assert (x.dt() == XTERNAL);

        TermContainer xx = x.subterms();
        for (Event<Term> r : byTerm.get(x.root())) {
            if (r.absolute()) {
                Term rt = r.id;
                if (rt.subterms().equals(xx)) {
                    if (!each.test(r))
                        return; //done
                }
            }

        }

        if (x.subs() == 2) {
            Term a = xx.sub(0);
            Term b = xx.sub(1);

            Collection<Event<Term>> aTerms = byTerm.get(a);
            Collection<Event<Term>> bTerms = !b.equals(a) ? byTerm.get(b) : List.of();
            int ns = aTerms.size() + bTerms.size();
            List<Event<Term>> sources = $.newArrayList(ns);
            sources.addAll(aTerms);
            sources.addAll(bTerms);
            Collections.shuffle(sources, random()); //TODO use (possibly biased) roulette sampling

            boolean repeat = a.unneg().equals(b.unneg()); //if true, then we must be careful when trying this in a commutive-like result which would collapse the two terms

            traverseDFS(sources, true, true,
                    new SolveDeltaTraverser(a, b, (long start, long ddt) -> {
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
                            return each.test(start != TIMELESS ? new Absolute(y, start) : new Unsolved(y, absolute));
                        } else {
                            return true;
                        }
                    }));
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

    public void solve(Term x, Predicate<Event<Term>> each) {
        solvable(x, e -> {

            if (Param.DEBUG) assert (e.id.root().equals(x.root()));

            if (e instanceof Absolute) {
                return each.test(e);
            } else if (e instanceof Unsolved) {

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
    public void solvable(Term x, Predicate<Event<Term>> each) {

        //1. check if the term is known directly
        for (Event e : byTerm.get(x)) {
            if (e.absolute() && !each.test(e))
                return; //done
        }


        Map<Term, Term> xternals = new UnifiedMap();

        Map<Term, LongSet> absolute = new UnifiedMap();
        absolute.put(x, EMPTY_LONG_SET);

        final boolean[] clue = {false};
        x.recurseTerms(term -> true, xs -> {
            if (xs.dt() == XTERNAL) {
                xternals.put(xs, xs);
            } else {

                absolute.computeIfAbsent(xs, s -> {
                    Collection<Event<Term>> xe = byTerm.get(s);
                    if (xe != null) {
                        LongHashSet l = new LongHashSet();
                        xe.forEach(e -> {
                            if (e.absolute())
                                l.add(e.start());
                        });

                        if (!l.isEmpty()) {
                            clue[0] = true;
                            return l.toImmutable();
                        }
                    }

                    return EMPTY_LONG_SET;
                });
            }
            return true;
        }, null);

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
            solveDT(x, absolute, each); //resolve any XTERNAL at top-level
        } else {
            solve(new Unsolved(x, absolute), each);
        }
    }

    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    private boolean solve(Unsolved<Term> u, Predicate<Event<Term>> each) {

        Map<Term, LongSet> a = u.absolute;


//        Stream<Event<Term>> src = a.entrySet().stream().flatMap(aa -> {
//            Term aaa = aa.getKey();
//            if (aa.getValue() == EMPTY_LONG_SET)
//                return byTerm.get(aaa).stream();
//            else
//                return Stream.empty();
//        });

        Term targetTerm = u.id;

//        Relative target = (Relative) know(targetTerm);  //add the target to the graph

        //find paths from the KNOWN to the UNKNOWN
        //sources.forEach(aa -> {

//            ArrayList<Event<Term>> as = Lists.newArrayList(
//                    Iterables.filter(byTerm.get(aa), Event::absolute));


        traverseDFS(
                u,
                true, true,
                new SolveNodeTraverser(a.keySet(), each));
        //});


        return true;
    }

//    @Override
//    protected Node<Event<Term>, TimeSpan> newNode(Event<Term> data) {
//        if (data instanceof Relative) {
//            return new RelativeNode(data);
//        } else {
//            return super.newNode(data);
//        }
//    }

    abstract protected class SolveTraverser extends Search<Event<Term>, TimeSpan> {

        protected SolveTraverser() {
            super();
        }

        public Stream<Edge<Event<Term>, TimeSpan>> dynamicLink(Node<Event<Term>, TimeSpan> n, Predicate<Event<Term>> eventFilter) {


            Term nt = n.id.id;
            return Util.buffer(byTerm.get(nt).stream()
                    .filter(x -> !x.equals(n.id))
                    .filter(eventFilter).map(e -> {
                                Node<Event<Term>, TimeSpan> that = TimeGraph.this.node(e);
                                return new Edge<>(n, that,
//                                outOrIn ? n : that,
//                                outOrIn ? that : n,
                                        TS_ZERO);
                            }
                    ));
        }


        public long startTime(FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {
            BooleanObjectPair<Edge<Event<Term>, TimeSpan>> firstStep = path.get(0);
            boolean outOrIn = firstStep.getOne();
            return firstStep.getTwo().from(outOrIn).id.start();
        }

        public long pathDT(FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {

            long t = 0;
            //compute relative path
            boolean dternal = false;
            for (int i = 0, pathSize = path.size(); i < pathSize; i++) {
                BooleanObjectPair<Edge<Event<Term>, TimeSpan>> r = path.get(i);
                long spanDT = r.getTwo().get().dt;
                if (spanDT == ETERNAL) {
                    //no change, crossed a DTERNAL step
                    dternal = true;
                } else if (spanDT != 0) {
                    t += spanDT * (r.getOne() ? +1 : -1);
                }
            }

            return t;
        }

    }

    protected class SolveNodeTraverser extends SolveTraverser {

        private final Set<Term> targets;
        private final Predicate<Event<Term>> each;

        public SolveNodeTraverser(Set<Term> targets, Predicate<Event<Term>> each) {
            this.targets = targets;
            this.each = each;
        }

        @Override
        protected Stream<Edge<Event<Term>, TimeSpan>> edges(Node<Event<Term>, TimeSpan> n, boolean in, boolean out) {
            return Stream.concat(
                    super.edges(n, in, out),
                    dynamicLink(n, e -> true)
            ).distinct();
        }


        @Override
        public boolean visit(Node<Event<Term>, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {
            if (path.isEmpty())
                return true;

            //System.out.println(path);

            Term current = n.id.id;
            if (n.id.absolute()) {
                long pathEndTime = n.id.start();
                BooleanObjectPair<Edge<Event<Term>, TimeSpan>> pathStart = path.get(0);
                Term pathStartTerm = pathStart.getTwo().from(pathStart.getOne()).id.id;
                if (!each.test(new Absolute(pathStartTerm,
                        pathEndTime == ETERNAL ? ETERNAL :
                                pathEndTime - pathDT(path))))
                    return false;
            }

            return true;
        }
    }

    protected class SolveDeltaTraverser extends SolveTraverser {

        private final Term a, b;
        private final LongLongPredicate each;

        public SolveDeltaTraverser(Term a, Term b, LongLongPredicate each) {
            this.a = a;
            this.b = b;
            this.each = each;
        }

        @Override
        protected Stream<Edge<Event<Term>, TimeSpan>> edges(Node<Event<Term>, TimeSpan> n, boolean in, boolean out) {
            return Stream.concat(super.edges(n, in, out), dynamicLink(n, e -> true)).distinct();
        }

        @Override
        public boolean visit(Node<Event<Term>, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {

            if (path.isEmpty())
                return true;

            //System.out.println(path);

            Term endTerm = n.id.id;
            boolean endA = a.equals(endTerm);
            boolean endB = b.equals(endTerm);

            if (endA || endB) {
                BooleanObjectPair<Edge<Event<Term>, TimeSpan>> startStep = path.getFirst();
                BooleanObjectPair<Edge<Event<Term>, TimeSpan>> endStep = path.getLast();
                Edge<Event<Term>, TimeSpan> startEdge = startStep.getTwo();
                Edge<Event<Term>, TimeSpan> endEdge = endStep.getTwo();

                Event<Term> startEvent = startEdge.from(startStep.getOne()).id;
                Event<Term> endEvent = endEdge.to(endStep.getOne()).id;
                Term startTerm = startEvent.id;

                if ((endA && startTerm.equals(b)) || (endB && startTerm.equals(a))) {


                    long startTime = startEvent.absolute() ? startEvent.start() : TIMELESS;
                    long endTime = endEvent.absolute() ? endEvent.start() : TIMELESS;

                    long dt;
                    if (startEvent.absolute() && endEvent.absolute()) {
                        //use the two endpoints and subtract the dt

                        dt = endTime - startTime;
                    } else {

                        //TODO more rigorous traversal of the dt chain
                        //compute from one end to the other, summing dt in the correct direction along the way
                        //special handling for encountered absolute terms and DTERNAL

                        dt = pathDT(path);
                    }

                    if (endA && dt != ETERNAL)
                        dt = -dt; //reverse

                    if (!each.accept(startTime, dt))
                        return false;
                }
            }

            return true;
        }
    }

//    private class RelativeNode extends Node<Event<Term>, TimeSpan> {
//        public RelativeNode(Event<Term> data) {
//            super(data);
//        }
//
////        final Collection<Edge<Event<Term>, TimeSpan>> outEnhanced = new ForwardingList<>() {
////            @Override public Iterator<Edge<Event<Term>, TimeSpan>> iterator() {
////                final Term thisTerm = RelativeNode.this.get().id();
////                return Iterators.concat(
////                        Iterators.transform(
////                            Iterators.filter(byTerm.get(thisTerm).iterator(),
////                                x -> x.getClass() == Event.class),
////                            e -> new Edge<>(RelativeNode.this, node(e.id()), TimeSpan.the(0, 1f))
////                        ),
////                        RelativeNode.super.out().iterator()
////                );
////            }
////
////            @Override
////            protected List<Edge<Event<Term>, TimeSpan>> delegate() {
////                return (List) RelativeNode.this.out();
////            }
////        };
//
//        Stream<Edge<Event<Term>, TimeSpan>> dynamic(boolean outOrIn) {
//
//            return byTerm.get(RelativeNode.this.get().id()).stream().filter(
//                    Event::absolute).map(
//                    e -> {
//                        Node<Event<Term>, TimeSpan> that = TimeGraph.this.add(e);
//                        return new Edge<>(
//                                outOrIn ? RelativeNode.this : that,
//                                outOrIn ? that : RelativeNode.this,
//                                new TimeSpan(0,1f));
//                    }
//            );
//        }
//
//
//        @Override
//        public Stream<Edge<Event<Term>, TimeSpan>> in() {
//            return Stream.concat(super.in(), dynamic(false));
//        }
//
//        @Override
//        public Stream<Edge<Event<Term>, TimeSpan>> out() {
//            return Stream.concat(super.out(), dynamic(true));
//        }
//    }
}
