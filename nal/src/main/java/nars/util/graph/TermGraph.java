package nars.util.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import jcog.pri.Priority;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.TruthFunctions;

import java.util.Iterator;
import java.util.Set;

import static nars.Op.IMPL;
import static nars.time.Tense.DTERNAL;

public enum TermGraph {
    ;


    public static MutableValueGraph<Term, Float> termlink(NAR nar) {
        MutableValueGraph<Term, Float> g = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
        return termlink(nar, g);
    }

    private static MutableValueGraph<Term, Float> termlink(NAR nar, MutableValueGraph<Term, Float> g) {
        nar.forEachConceptActive(cf -> {
            Concept c = cf.get();
            Term s = c.term();
            g.addNode(s);
            c.termlinks().forEach(tl -> {
                Term t = tl.get();
                g.addNode(t);
                float p = tl.pri();
                if (p==p)
                    g.putEdgeValue(s, t, p);
            });
        });
        return g;
    }


    public static class ImplGraph  {

        //final static String VERTEX = "V";

        public ImplGraph() {
            super();
//            nar.onTask(t -> {
//                if (t.isBelief())
//                    task(nar, t);
//            });

        }

        protected boolean accept(Task t) {
            //example:
            return t.op() == IMPL;
        }

        public MutableValueGraph<Term, Float> snapshot(Iterable<? extends Term> sources, NAR nar, long when) {
            return snapshot(null, sources, nar, when);
        }

        public MutableValueGraph<Term, Float> snapshot(MutableValueGraph<Term, Float> g, Iterable<? extends Term> sources, NAR nar, long when) {

            if (g == null) {
                g = ValueGraphBuilder
                        .directed()
                        .allowsSelfLoops(true).build();
            }

            Set<Term> done = Sets.newConcurrentHashSet();

            //TODO bag for pending concepts to visit?
            Set<Term> next = Sets.newConcurrentHashSet();
            Iterables.addAll(next, sources);

            int maxSize = 1024;
            do {
                Iterator<Term> ii = next.iterator();
                while (ii.hasNext()) {
                    Term t = ii.next();
                    ii.remove();
                    if (!done.add(t))
                        continue;
                    recurseTerm(nar, when, g, done, next, t);
                }
            } while (!next.isEmpty() && g.nodes().size() < maxSize);

            return g;
        }

        void recurseTerm(NAR nar, long when, MutableValueGraph<Term, Float> g, Set<Term> done, Set<Term> next, Term t) {


            Concept tc = nar.concept(t);
            if (tc == null)
                return; //ignore non-conceptualized

            tc.termlinks().forEachKey(m -> {

                        if (m.op() == IMPL /* && m.vars()==0 */
                            //&& ((Compound)m).containsTermRecursively(t)) {
                                ) {
                            Compound l = (Compound) m;
                            Term s = l.sub(0);

                            Term p = l.sub(1);

                            //if (!g.nodes().contains(s) || !done.contains(p)) {
                            if ((s.equals(t) || s.containsRecursively(t)) ||
                                    (p.equals(t) || p.containsRecursively(t))) {
                                next.add(s);
                                next.add(p);
                                impl(g, nar, when, l, s, p);
                            }
                            //}
                        }
                    }
            );
        }

        private void impl(MutableValueGraph<Term, Float> g, NAR nar, long when, Compound l, Term subj, Term pred) {
            Concept c = nar.concept(l);
            if (c == null)
                return;

            int dur = nar.dur();
            Task t = c.beliefs().match(when, dur);
            if (t == null)
                return;

            int dt = t.dt();
            boolean reverse;
            if (dt != DTERNAL && (dt < 0)) {
                dt = -dt;
                reverse = true;
            } else {
                reverse = false;
            }
            float evi =
                    t.evi(when, dur);
            //dt!=DTERNAL ? w2c(TruthPolation.evidenceDecay(t.evi(), dur, dt)) : t.conf();

            float freq = t.freq();
            boolean neg;
            float val = (TruthFunctions.expectation(freq, evi) - 0.5f) * 2f;
            if (val < 0f) {
                val = -val;
                neg = true;
            } else {
                neg = false;
            }
            if (val < Priority.EPSILON)
                return;

            Term S = reverse ? $.negIf(pred, neg) : subj;
            Term P = reverse ? subj : $.negIf(pred, neg);
            g.putEdgeValue(S, P, val + g.edgeValueOrDefault(S, P, 0f));

        }

    }

}

//    public static final class ImplLink extends RawPLink<Term> {
//
//        final boolean subjNeg;
//        final boolean predNeg;
//
//        public ImplLink(Term o, float p, boolean subjNeg, boolean predNeg) {
//            super(o, p);
//            this.subjNeg = subjNeg;
//            this.predNeg = predNeg;
//        }
//
//        @Override
//        public boolean equals(@NotNull Object that) {
//            return super.equals(that) && ((ImplLink)that).subjNeg == subjNeg;
//        }
//
//        @Override
//        public int hashCode() {
//            return super.hashCode() * (subjNeg ? -1 : +1);
//        }
//
//    }
//
//    class ConceptVertex  {
//
//        //these are like more permanent set of termlinks for the given context they are stored by
//        final HijackBag<Term, ImplLink> in;
//        final HijackBag<Term, ImplLink> out;
//
//        public ConceptVertex(Random rng) {
//            in = new MyPLinkHijackBag(rng);
//            out = new MyPLinkHijackBag(rng);
//        }
//
//        private class MyPLinkHijackBag extends PLinkHijackBag {
//            public MyPLinkHijackBag(Random rng) {
//                super(32, 4, rng);
//            }
//
//            @Override
//            public float pri(@NotNull PLink key) {
//                float p = key.pri();
//                return Math.max(p - 0.5f, 0.5f - p); //most polarizing
//            }
//
//            @Override
//            protected float merge(@Nullable PLink existing, @NotNull PLink incoming, float scale) {
//
//                //average:
//                if (existing != null) {
//                    float pAdd = incoming.priSafe(0);
//                    existing.priAvg(pAdd, scale);
//                    return 0;
//                } else {
//                    return 0;
//                }
//
//            }
//        }
//    }

