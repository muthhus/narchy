package nars.util.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import jcog.data.graph.AdjGraph;
import jcog.pri.Priority;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.TruthPolation;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.TruthFunctions;

import java.util.Iterator;
import java.util.Set;

import static nars.Op.IMPL;
import static nars.time.Tense.DTERNAL;
import static nars.truth.TruthFunctions.w2c;

public enum TermGraph {
    ;

    public static AdjGraph<Term, Float> termlink(NAR nar) {
        AdjGraph<Term, Float> g = new AdjGraph<>(true);
        return termlink(nar, g);
    }

    private static AdjGraph<Term, Float> termlink(NAR nar, AdjGraph<Term, Float> g) {

        nar.conceptActive().forEach(cf -> {
            Concept c = cf.get();
            Term s = c.term();
            g.addNode(s);
            c.termlinks().forEach(tl -> {
                Term t = tl.get();
                if (t.equals(s))
                    return; //no self loop
                g.addNode(t);
                float p = tl.pri();
                if (p == p)
                    g.setEdge(s, t, p);
            });
        });
        return g;
    }


    public static class ImplGraph {

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

        public AdjGraph<Term, Float> snapshot(Iterable<Term> sources, NAR nar, long when) {
            return snapshot(null, sources, nar, when);
        }

        public AdjGraph<Term, Float> snapshot(AdjGraph<Term, Float> g, Iterable<Term> sources, NAR nar, long when) {

            if (g == null) {
                g = new AdjGraph<>(true);
            }

            @Deprecated Set<Term> done = Sets.newConcurrentHashSet();

            //TODO bag for pending concepts to visit?
            Set<Termed> next = Sets.newConcurrentHashSet();
            Iterables.addAll(next, sources);

            int maxSize = 1024;
            do {
                Iterator<Termed> ii = next.iterator();
                while (ii.hasNext()) {
                    Term t = ii.next().term();
                    ii.remove();
                    if (!done.add(t))
                        continue;
                    recurseTerm(nar, when, g, done, next, t);
                }
            } while (!next.isEmpty() && g.nodeCount() < maxSize);

            return g;
        }

        protected void recurseTerm(NAR nar, long when, AdjGraph<Term, Float> g, Set<Term> done, Set<Termed> next, Term t) {



            Concept tc = nar.concept(t);
            if (tc == null)
                return; //ignore non-conceptualized

            tc.termlinks().forEach(ml -> {

                        Term l = ml.get();
                        if (l.op() == IMPL && !l.hasVarQuery() && l.subterms().containsRecursively(t) /* && m.vars()==0 */
                            //&& ((Compound)m).containsTermRecursively(t)) {
                                ) {

                            Term s = l.sub(0);
                            if (!acceptTerm(s))
                                return;

                            Term p = l.sub(1);
                            if (!acceptTerm(p))
                                return;

                            //if (!g.nodes().contains(s) || !done.contains(p)) {
//                            if ((s.equals(t) || s.containsRecursively(t)) ||
//                                    (p.equals(t) || p.containsRecursively(t))) {
                                next.add(s);
                                next.add(p);
                                impl(g, nar, when, l, s, p);
                           // }
                            //}
                        }
                    }
            );
        }

        protected boolean acceptTerm(Term p) {
            return true;
        }

        private void impl(AdjGraph<Term, Float> g, NAR nar, long when, Term l, Term subj, Term pred) {

            int dur = nar.dur();
            Task t = nar.belief(l, when);
            if (t == null)
                return;

            int dt = t.dt();
            if (dt == DTERNAL)
                dt = 0;

            float evi =
                    t.evi(when, dur);
            //dt!=DTERNAL ? w2c(TruthPolation.evidenceDecay(t.evi(), dur, dt)) : t.conf();

            float freq = t.freq();
            boolean neg;
            float val = (freq - 0.5f) * 2f * evi;
            if (val < 0f) {
                val = -val;
                neg = true;
            } else {
                neg = false;
            }

            val *= TruthPolation.evidenceDecay(1f, dur, Math.abs(dt));

            if (val!=val || val < Priority.EPSILON)
                return;

            boolean reverse = dt < 0;
            Term S = reverse ? pred.negIf(neg) : subj;
            Term P = reverse ? subj : pred.negIf(neg);
            g.addNode(S);
            g.addNode(P);
            g.setEdge(S, P, val + g.edge(S, P, 0f));
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

