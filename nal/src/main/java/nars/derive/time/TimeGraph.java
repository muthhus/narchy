package nars.derive.time;

import astar.model.TimeProblem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jcog.data.graph.hgraph.Node;
import nars.$;
import nars.Narsese;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static jcog.Texts.n2;
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
        public final float weight;

        public TimeSpan(long dt, float weight) {
            this.dt = dt;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return (dt == ETERNAL ? "~" : (dt >= 0 ? ("+"+dt) : ("-"+(-dt)))) + (weight!=1 ? "x" + n2(weight) : "");
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

    public void link(Event<Term> x, long dt, float weight, Event<Term> y) {
        link(x, new TimeSpan(dt, weight), y);
    }

    @Override
    protected void onAdd(Node<Event<Term>, TimeSpan> x) {
        Event<Term> e = x.get();
        Term t = e.id.getOne();

        byTerm.put(t, e);
        Term tRoot = t.root();
        if (!tRoot.equals(t))
            byTerm.put(tRoot, e);

        switch (t.op()) {
            case IMPL:

                int dt = t.dt();
                Term subj = t.sub(0);
                Term pred = t.sub(1);
                link(know(subj), dt == DTERNAL ? ETERNAL : (dt + subj.dtRange()), 1f, know(pred));

                break;
            case CONJ:
                TermContainer tt = t.subterms();
                if (t.dt() == DTERNAL) {
                    for (Term u : tt) {
                        link(know(u), ETERNAL, 1f, e);
                    }
                } else {

                    long et = e.start();
                    if (et == TIMELESS || et == ETERNAL) {
                        //only chain sibling events together
                        int s = tt.subs();
                        Term pp = tt.sub(0);
                        Event p = know(pp);
                        long pt = t.subTime(pp);
                        for (int i = 1; i < s; i++) {
                            Term rr = tt.sub(i);
                            Event r = know(rr);
                            long rt = t.subTime(rr);
                            long delta = rt - pt;
                            //link
                            link(p, delta, 1f, r);
                            p = r;
                            pt = rt;
                        }

                    } else {
                        //locate the events absolutely
                        t.eventsWhile((w, y) -> {
                            know(y, w);
                            return true;
                        }, et, false, false, 0);
                    }

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
            MutableSet<Event<Term>> ae = absolutes(a);
            if (!ae.isEmpty()) {
                Term b = xx.sub(1);
                MutableSet<Event<Term>> be = absolutes(b);
                if (!be.isEmpty()) {
                    //cartesian product of the two, maybe prioritized by least distance?
                    LazyIterable<Pair<Event<Term>, Event<Term>>> matrix = Sets.cartesianProduct(ae, be);

                    matrix.allSatisfy(ab->{

                        long bt = ab.getTwo().start();
                        long at = ab.getOne().end();

                        int dt;
                        if (bt == ETERNAL || at == ETERNAL) {
                            dt = DTERNAL;
                        } else {
                            long ddt = bt - at;
                            assert(Math.abs(ddt) < Integer.MAX_VALUE);
                            dt = (int)ddt;
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

    public MutableSet<Event<Term>> absolutes(Term a) {
        UnifiedSet<Event<Term>> ae = new UnifiedSet();
        solve(a, (e) -> {
            if (e.start()!=TIMELESS && ae.add(e))
                return ae.size() < MAX_SUBTERM_SOLUTIONS;
            else
                return true;
        });
        return ae;
    }

    final static LongSet EMPTY_LONG_SET = LongSets.immutable.empty();

    /** each should only receive Event or Unsolved instances, not Relative's  */
    public void solve(Term x, Predicate<Event<Term>> each) {

        //1. check if the term is known directly
        for (Event e : byTerm.get(x)) {
            if (e.start()!=TIMELESS && !each.test(e))
                return; //done
        }

        if (!x.isTemporal())
            return; //nothing further to do

        Map<Term, Term> unknowns = new UnifiedMap();
        Map<Term, LongSet> absolute = new UnifiedMap();
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
                            if (!(e instanceof Relative))
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

}
