package nars.util.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.bag.RawPLink;
import jcog.bag.impl.PLinkHijackBag;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import org.jgrapht.DirectedGraph;

import java.util.*;

public abstract class TermGraph {

    class ConceptVertex extends RawPLink<Term> {

        //these are like more permanent set of termlinks for the given context they are stored by
        final PLinkHijackBag<Term> in;
        final PLinkHijackBag<Term> out;

        public ConceptVertex(Term c, Random rng) {
            super(c, 1f);
            in = new PLinkHijackBag(32, 4, rng);
            out = new PLinkHijackBag(32, 4, rng);
        }

    }


    protected TermGraph(Random rng) {

    }

    public static class StatementGraph extends TermGraph {

        final static String VERTEX = "V";

        public StatementGraph(NAR nar) {
            super(nar.random);
            nar.onTask(t -> {
                if (accept(t)) {
                    Term subj = t.term(0).unneg();
                    Concept sc = nar.concept(subj);
                    if (sc == null) return;

                    Term pred = t.term(1).unneg();
                    Concept pc = nar.concept(pred);
                    if (pc == null) return;

                    if (sc.equals(pc)) return; //self-loop, maybe allow

                    ConceptVertex sv = sc.meta(VERTEX,
                            (k, p) -> new ConceptVertex(sc.term(), nar.random));
                    sv.out.put(new RawPLink(pc.term(), 1f));

                    ConceptVertex pv = pc.meta(VERTEX,
                            (k, p) -> new ConceptVertex(pc.term(), nar.random));
                    pv.in.put(new RawPLink(sc.term(), 1f));

                }
            });

        }

        protected boolean accept(Task t) {
            //example:
            return t.op() == Op.IMPL;
        }

        public MutableGraph<Term> snapshot(Iterable<? extends Term> sources, NAR nar) {
            MutableGraph<Term> g = GraphBuilder.directed().allowsSelfLoops(false).build();

            //TODO bag for pending concepts to visit?
            Set<Term> next = Sets.newConcurrentHashSet();
            Iterables.addAll(next, sources);

            int maxLoops = 16;
            do {
                Iterator<? extends Term> ii = next.iterator();
                while (ii.hasNext()) {
                    Term t = ii.next();
                    recurseTerm(nar, g, next, t);
                    ii.remove();
                }
            } while (--maxLoops > 0 && !next.isEmpty());

            return g;
        }

        public void recurseTerm(NAR nar, MutableGraph<Term> g, Set<Term> next, Term t) {
            Concept tc = nar.concept(t);
            if (tc == null)
                return; //ignore non-conceptualized

            ConceptVertex v = tc.get(VERTEX);
            if (v != null) {
                if (g.addNode(t)) {

                    v.in.forEach(x -> {
                        Term tt = x.get();
                        if (!tt.equals(t)) {
                            if (g.putEdge(tt, t))
                                next.add(tt);
                        }
                    });
                    v.out.forEach(x -> {
                        Term tt = x.get();
                        if (!tt.equals(t)) {
                            if (g.putEdge(t, tt))
                                next.add(tt);
                        }
                    });
                }
            }
        }

    }

}
