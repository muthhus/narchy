package nars.derive.time;

import astar.model.TimeProblem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import jcog.TODO;
import jcog.data.graph.hgraph.Edge;
import jcog.data.graph.hgraph.HashGraph;
import jcog.data.graph.hgraph.Node;
import jcog.list.FasterList;
import nars.$;
import nars.Narsese;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
 * for precision, events with implicit temporal range are
 * represented as a pair of start/stop points.
 * <p>
 * an ETERNITY event can be used as an atemporal reference
 * separate from time=0.
 * <p>
 * likewise, DTERNAL relationships can be maintained separate
 * from +0.
 */
public class TimeGraph extends TimeProblem<Term, TimeGraph.TimeSpan> {

    public static class TimeSpan {
        public final long dt;
        //public final float weight;

        public TimeSpan(long dt) {
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

    public Event know(Term t) {
        assert (t.op().conceptualizable);
        Event e = new Relative(t);
        return (Event) add(e).get();
    }

    public static class TermEvent extends Event<Term> {
        public TermEvent(Term id, long when) {
            super(id, when);
            assert (when != TIMELESS);
        }

        @Override
        public long end() {
            long s = super.start();
            if (s != ETERNAL) {
                Term t = id();
                return s + t.dtRange();
            } else {
                return s;
            }
        }
    }

    public Event know(Term t, long start) {
        assert (t.op().conceptualizable);
        assert (start != TIMELESS);
        Event e = new TermEvent(t, start);
        return (Event) add(e).get();
    }

    public void link(Event<Term> x, long dt, Event<Term> y) {

        link(x, new TimeSpan(dt), y);
    }

    @Override
    protected void onAdd(Node<Event<Term>, TimeSpan> x) {
        Event<Term> event = x.get();
        Term eventTerm = event.id.getOne();

        byTerm.put(eventTerm, event);
        Term tRoot = eventTerm.root();
        if (!tRoot.equals(eventTerm))
            byTerm.put(tRoot, event);

        switch (eventTerm.op()) {
            case IMPL: {

                int dt = eventTerm.dt();
                Term subj = eventTerm.sub(0);
                Term pred = eventTerm.sub(1);
                link(know(subj), dt == DTERNAL ? ETERNAL : (dt + subj.dtRange()), know(pred));

            }
            break;
            case CONJ:
                TermContainer tt = eventTerm.subterms();
                long et = event.start();

                int eventDT = eventTerm.dt();

                int s = tt.subs();
                if (et == TIMELESS) {
                    //chain the sibling subevents
                    if (s == 2) {
                        Term se0 = tt.sub(0);
                        Event e0 = know(se0);
                        Term se1 = tt.sub(1);
                        Event e1 = know(se1);
                        int dt;
                        Event earliest;
                        if (eventDT == DTERNAL) {
                            dt = DTERNAL;
                            earliest = e0; //just use the first by default
                        } else {
                            long t0 = eventTerm.subTime(se0);
                            long t1 = eventTerm.subTime(se1);
                            long ddt = (int) (t1 - t0);
                            assert (ddt < Integer.MAX_VALUE);
                            dt = (int) ddt;
                            earliest = t0 < t1 ? e0 : e1;
                        }
                        link(e0, dt, e1);
                        link(earliest, 0, event);

                    } else {
                        throw new TODO();
                    }

                } else if (et == ETERNAL || eventDT == DTERNAL) {
                    //chain the children subevents events
                    for (int i = 0; i < s; i++) {
                        Term rr = tt.sub(i);
                        Event subEvent = know(rr);
                        long rt = eventDT == DTERNAL ? ETERNAL : eventTerm.subTime(rr);
                        link(event, rt, subEvent);
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

    public void solveDT(Term x, Predicate<Term> each) {
        assert (x.dt() == XTERNAL);

        TermContainer xx = x.subterms();
        for (Event<Term> r : byTerm.get(x.root())) {
            Term rt = r.id();
            if (rt.subterms().equals(xx)) {
                if (!each.test(rt))
                    return; //done
            }
        }

        if (x.subs() == 2) {
            Term a = xx.sub(0);
            Set<Event<Term>> ae = absolutes(a);
            if (!ae.isEmpty()) {
                Term b = xx.sub(1);
                Set<Event<Term>> be = absolutes(b);
                if (!be.isEmpty()) {
                    //cartesian product of the two, maybe prioritized by least distance?
                    LazyIterable<Pair<Event<Term>, Event<Term>>> matrix = Sets.cartesianProduct(ae, be);
                    matrix.allSatisfy(ab -> {

                        long bt = ab.getTwo().start();
                        long at = ab.getOne().end();

                        int dt;
                        if (bt == ETERNAL || at == ETERNAL) {
                            dt = DTERNAL;
                        } else {
                            long ddt = bt - at;
                            assert (Math.abs(ddt) < Integer.MAX_VALUE);
                            dt = (int) ddt;
                        }

                        Term tt = x.dt(dt);
                        if (tt.op().conceptualizable)
                            return each.test(tt);
                        else
                            return true;
                    });
                }
            }
        } else {
            //?
        }
    }

    public Set<Event<Term>> absolutes(Term a) {
        UnifiedSet<Event<Term>> ae = new UnifiedSet();
        solve(a, (e) -> {
            if (e.start() != TIMELESS && ae.add(e))
                return ae.size() < MAX_SUBTERM_SOLUTIONS;
            else
                return true;
        });
        return ae.isEmpty() ? Set.of() : ae;
    }

    final static LongSet EMPTY_LONG_SET = LongSets.immutable.empty();

    public void solve(Term x, Predicate<Event<Term>> each) {
        solvable(x, e -> {
            if (!(e instanceof Unsolved)) {
                return each.test(e);
            } else {
                if (!solve((Unsolved) e, each))
                    return false;
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

        if (!x.isTemporal())
            return; //nothing further to do

        Map<Term, Term> unknowns = new UnifiedMap();

        Map<Term, LongSet> absolute = new UnifiedMap();
        absolute.put(x, EMPTY_LONG_SET);

        final boolean[] clue = {false};
        x.recurseTerms(term -> true, xs -> {
            if (xs.dt() == XTERNAL) {
                unknowns.put(xs, xs);
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
                        } else {
                            return EMPTY_LONG_SET;
                        }

                    } else {
                        return EMPTY_LONG_SET;
                    }
                });
            }
            return true;
        }, null);

        if (unknowns.isEmpty()) {
            if (!clue[0])
                return; //no temporal basis to compute

            if (x.dt() == XTERNAL) {
                solveDT(x, y -> {
                    each.test(new Unsolved(y, absolute));
                    return true;
                });
            } else {
                each.test(new Unsolved(x, absolute));
            }
        } else {
            final boolean[] foundAny = {false};
            unknowns.replaceAll((u, z) -> {
                final Term[] vv = new Term[1];
                solveDT(u, (v) -> {
                    vv[0] = v;
                    return false; //just one for now
                });
                if (vv[0] != null) {
                    foundAny[0] = true;
                    return vv[0];
                } else {
                    return u;
                }
            });
            if (foundAny[0]) {
                Term y = x.replace(unknowns);
                assert (!(y.equals(x)));
                if (y.op().conceptualizable)
                    each.test(new Unsolved(y, absolute));
            }
        }

    }

    /**
     * solves the start time for the given Unsolved event.  returns whether callee should continue iterating
     */
    private boolean solve(Unsolved<Term> u, Predicate<Event<Term>> each) {

        Map<Term, LongSet> a = u.absolute;

        Set<Term> targets = new UnifiedSet();
        List<Term> sources = new FasterList();
        for (Map.Entry<Term, LongSet> aa : a.entrySet()) {
            if (aa.getValue() == EMPTY_LONG_SET) {
                targets.add(aa.getKey());
            } else {
                sources.add(aa.getKey());
            }
        }

        sources.forEach(aa ->
            traverseDFS( Iterables.filter(byTerm.get(aa), Event::absolute), true, true, new SolveTraverser(targets) )
        );


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

    public static void main(String[] args) throws Narsese.NarseseException {

        TimeGraph t = new TimeGraph();
//        t.know($.$("(a ==>+1 b)"), ETERNAL);
//        t.know($.$("(b ==>+1 (c &&+1 d))"), 0);
//        t.know($.$("(a &&+1 b)"), 4);

        t.know($.$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        t.know($.$("one"), 1);
        t.know($.$("two"), 20);

        t.print();

        System.out.println();

        for (String s : List.of("one", "(one &&+1 two)", "(one &&+- two)", "(one ==>+- three)")) {
            Term x = $.$(s);
            System.out.println("SOLVE: " + x);
            t.solve(x, (y) -> {
                System.out.println("\t" + y);
                return true;
            });
        }


    }

    protected class SolveTraverser implements DFSTraverser<Event<Term>,TimeSpan> {

        private final Set<Term> targets;

        public SolveTraverser(Set<Term> targets) {
            this.targets = targets;
        }

        public Stream<Edge<Event<Term>, TimeSpan>> dynamic(Node<Event<Term>, TimeSpan> n, boolean outOrIn) {

            Term nt = n.get().id();
            return byTerm.get(nt).stream().filter(e -> !e.absolute()).map(
                    e -> {
                        Node<Event<Term>, TimeSpan> that = TimeGraph.this.add(e);
                        return new Edge<>(
                                outOrIn ? n : that,
                                outOrIn ? that : n,
                                new TimeSpan(0));
                    }
            );
        }

        @Override
        public Stream<Edge<Event<Term>, TimeSpan>> edges(Node<Event<Term>, TimeSpan> n, boolean outOrIn) {
            return Stream.concat(n.out(), dynamic(n, outOrIn));
        }

        @Override
        public boolean visit(Node<Event<Term>, TimeSpan> n, FasterList<BooleanObjectPair<Edge<Event<Term>, TimeSpan>>> path) {
            if (path.isEmpty())
                return true;

            System.out.println(path);

            Term target = n.get().id();
            if (targets.contains(target)) {
                boolean outOrIn = path.get(0).getOne();
                Edge<Event<Term>, TimeSpan> eStart = path.get(0).getTwo();
                Node<Event<Term>, TimeSpan> nStart = eStart.node(!outOrIn);
                long t = nStart.get().start();
                if (t != ETERNAL) {
                    //compute relative path
                    boolean dternal = false;
                    for (int i = 1, pathSize = path.size(); i < pathSize; i++) {
                        BooleanObjectPair<Edge<Event<Term>, TimeSpan>> r = path.get(i);
                        TimeSpan span = r.getTwo().get();
                        if (span.dt == ETERNAL) {
                            t += 0;
                            dternal = true;
                        } else if (t != 0) {
                            t += span.dt * (outOrIn ? +1 : -1);
                        }
                    }
                }
                know(target, t);
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
