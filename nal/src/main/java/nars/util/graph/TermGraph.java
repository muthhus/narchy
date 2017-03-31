package nars.util.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import jcog.bag.RawPLink;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.PLinkHijackBag;
import nars.*;
import nars.concept.Concept;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import static nars.Op.NEG;
import static nars.time.Tense.ETERNAL;

public abstract class TermGraph {

    public static final class ImplLink extends RawPLink<Term> {

        public final boolean neg;

        public ImplLink(Term o, float p, boolean neg) {
            super(o, p);
            this.neg = neg;
        }

        @Override
        public boolean equals(@NotNull Object that) {
            return super.equals(that) && ((ImplLink)that).neg == neg;
        }

        @Override
        public int hashCode() {
            return super.hashCode() * (neg ? -1 : +1);
        }

    }

    class ConceptVertex  {

        //these are like more permanent set of termlinks for the given context they are stored by
        final HijackBag<Term, ImplLink> in;
        final HijackBag<Term, ImplLink> out;

        public ConceptVertex(Random rng) {
            in = new PLinkHijackBag(32, 4, rng);
            out = new PLinkHijackBag(32, 4, rng);
        }

    }


    protected TermGraph() {

    }

    public static class ImplGraph extends TermGraph {

        final static String VERTEX = "V";

        public ImplGraph(NAR nar) {
            super();
            nar.onTask(t -> {
                if (t.isBelief())
                    task(nar, t);
            });

        }

        public void task(NAR nar, Task t) {
            int dt = t.dt();
            if (t.op() == Op.EQUI) {
                impl(nar, t, dt, t.term(0), t.term(1));
                impl(nar, t, dt, t.term(1), t.term(0));
            } else if (t.op() == Op.IMPL) {
                impl(nar, t, dt, t.term(0), t.term(1));
            }
        }

        public void impl(NAR nar, Task t, int dt, Term subj, Term pred) {
            if (dt != ETERNAL && dt < 0) {
                //TODO reverse implication
                impl(nar, t, -dt, pred, subj);
                return;
            }

            Concept sc = nar.concept(subj);
            if (sc == null) return;

            Concept pc = nar.concept(pred);
            if (pc == null) return;

            //if (sc.equals(pc)) return; //self-loop, maybe allow

            Truth tt = t.truth();
            float scale;
            boolean neg;
            if (tt.freq() >= 0.5f) {
                scale = TruthFunctions.expectation(tt.freq(), tt.conf());
            } else {
                scale = -TruthFunctions.expectation(1f - tt.freq(), tt.conf());
            }

            neg = (subj.op()==NEG);

            if (scale > Param.BUDGET_EPSILON) {
                ConceptVertex sv = sc.meta(VERTEX,
                        (k, p) -> {
                            if (p==null)
                                p = new ConceptVertex(nar.random);
                            return p;
                        });
                sv.out.put(new ImplLink(pc.term(), scale, neg));

                ConceptVertex pv = pc.meta(VERTEX,
                        (k, p) -> {
                            if (p==null)
                                p = new ConceptVertex(nar.random);
                            return p;
                        });
                pv.in.put(new ImplLink(sc.term(), scale, neg));
            }
        }

        protected boolean accept(Task t) {
            //example:
            return t.op() == Op.IMPL;
        }

        public MutableValueGraph<Term, Float> snapshot(Iterable<? extends Term> sources, NAR nar) {
            MutableValueGraph<Term, Float> g = ValueGraphBuilder.directed().allowsSelfLoops(false).build();

            //TODO bag for pending concepts to visit?
            Set<Term> next = Sets.newConcurrentHashSet();
            Iterables.addAll(next, sources);

            int maxSize = 1024;
            do {
                Iterator<Term> ii = next.iterator();
                while (ii.hasNext()) {
                    Term t = ii.next();
                    recurseTerm(nar, g, next, t);
                    ii.remove();
                }
            } while (!next.isEmpty() && g.nodes().size() < maxSize);

            return g;
        }

        public void recurseTerm(NAR nar, MutableValueGraph<Term,Float> g, Set<Term> next, Term t) {
            Concept tc = nar.concept(t);
            if (tc == null)
                return; //ignore non-conceptualized

            ConceptVertex v = tc.get(VERTEX);
            if (v != null) {
                if (g.addNode(t)) {

                    v.in.forEach(x -> {
                        Term tt = x.get();
                        if (!tt.equals(t)) {
                            if (g.putEdgeValue($.negIf(tt, x.neg), t, x.pri())==null)
                                next.add(tt);
                        }
                    });
                    v.out.forEach(x -> {
                        Term tt = x.get();
                        if (!tt.equals(t)) {
                            if (g.putEdgeValue($.negIf(t, x.neg), tt, x.pri())==null)
                                next.add(tt);
                        }
                    });
                }
            }
        }

    }

}
