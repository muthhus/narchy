package nars.derive.time;

import astar.model.TimeProblem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jcog.data.graph.hgraph.Node;
import nars.$;
import nars.Narsese;
import nars.term.Term;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static jcog.Texts.n2;
import static nars.time.Tense.DTERNAL;

/** represents a multigraph of events and their relationships
 *  calculates unknown times by choosing from the possible
 *  pathfinding results.
 *
 *  it can be used in various contexts:
 *      a) the tasks involved in a derivation
 *      b) as a general purpose temporal index, ie. as a meta-layer
 *         attached to one or more concept belief tables
 *
 * for precision, events with implicit temporal range are
 * represented as a pair of start/stop points.
 *
 * an ETERNITY event can be used as an atemporal reference
 * separate from time=0.
 *
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
            return (dt==ETERNAL ? "~" : (dt >= 0 ? "+" : "-") + dt ) + "x" + n2(weight);
        }
    }

    /** index by term */
    public final Multimap<Term, Event> byTerm = HashMultimap.create();

    /** index by term root, this can be lazily calculated */
    public Map<Term, TimeProblem.Event> byRoot = null;

    public TimeGraph() {

    }

    public Event know(Term t) {
        return know(t, ETERNAL);
    }

    public Event know(Term t, long start) {
        assert (t.op().conceptualizable);

        Event e;
        if (start == ETERNAL)
            e = new Relative(t);
        else
            e = new Event(t, start);
        return (Event) add(e).get();
    }

    public void link(Event<Term> x, long dt, float weight, Event<Term> y) {
        link(x, new TimeSpan(dt, weight), y);
    }

    @Override
    protected void onAdd(Node<Event<Term>, TimeSpan> x) {
        Event<Term> e = x.get();
        Term t = e.id.getOne();

        byTerm.put(t.root(), e);

        switch (t.op()) {
            case IMPL:

                int dt = t.dt();
                Term subj = t.sub(0);
                Term pred = t.sub(1);
                link(know(subj, ETERNAL), dt ==DTERNAL ? ETERNAL : (dt + subj.dtRange()), 1f, know(pred, ETERNAL));

                break;
            case CONJ:
                if (t.dt()==DTERNAL) {
                    for (Term u : t.subterms()) {
                        link(know(u, ETERNAL), ETERNAL,1f, e);
                    }
                } else {
                    t.eventsWhile((w, y) -> {
                        long et = e.when();
                        Event f = know(y, et != ETERNAL ? w + et : ETERNAL);
                        if (et == ETERNAL) {
                            link(f, w, 1f, e);
                        }
                        return true;
                    }, 0, true, false, 0);
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



    public void solve(Term t, Predicate<Event<Term>> each) {

        //1. check if the term is known directly
        for (Event e : byTerm.get(t.root())) {
            if (!each.test(e))
                return; //done
        }

    }

    public static void main(String[] args) throws Narsese.NarseseException {

        TimeGraph t = new TimeGraph();
//        t.know($.$("(a ==>+1 b)"), ETERNAL);
//        t.know($.$("(b ==>+1 (c &&+1 d))"), 0);
//        t.know($.$("(a &&+1 b)"), 4);

        t.know($.$("((one &&+1 two) ==>+1 (three &&+1 four))"));

        t.print();

        System.out.println();

        for (String s : List.of("one", "(one &&+1 two)", "(one &&+- two)", "(one ==>+- three)")) {
            Term x = nars.$.$(s);
            System.out.println("SOLVE: " + x);
            t.solve(x, (y) -> {
                System.out.println("\t" + y);
                return true;
            });
        }


    }
}
