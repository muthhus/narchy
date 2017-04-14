package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import jcog.Texts;
import jcog.list.FasterList;
import nars.*;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.test.agent.Line1DSimplest;
import nars.time.Tense;
import nars.util.graph.TermGraph;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static nars.NAgentX.runRT;
import static nars.Op.GOAL;
import static nars.Op.NEG;

/**
 * Created by me on 3/15/17.
 */
public class Line1D {

    public static void main(String[] args) {
        Param.DEBUG = true;


        NAR nar = runRT((NAR n) -> {

            //n.setControl(new InteractiveFirer(n));

            //n.log();

//            n.onTask(x -> {
//                if (x.isGoal() && !x.isInput()) {
//                    System.err.println(x.proof());
//                }
//            });

            n.termVolumeMax.setValue(24);
            n.beliefConfidence(0.9f);
            n.goalConfidence(0.9f);


            Line1DSimplest a = new Line1DSimplest(n);

            //n.log();

            implAccelerator(n, a);

            n.onTask(x -> {
                if (x.isGoal() && x instanceof DerivedTask) {
                    // && x.term().equals(a.o)
                    System.out.println(x.proof());
                }
            });


            //TEST CHEAT:
            //n.believe((Compound) $.impl($.inh($.diffe($.the("i"), $.the("o")), a.id), 1, a.out.term()), 1f, 0.99f);
            //n.believe((Compound) $.impl($.inh($.diffe($.the("o"), $.the("i")), a.id), 1, a.out.term()), 0f, 0.99f);


            n.onCycle(() -> {
                a.i.setValue(
                        0.5f * (Math.sin(n.time() / 20f) + 1f)
                        //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                        //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                );
            });

            //a.curiosityProb.setValue(0f);

            return a;

        }, 20, 3, -1);

    }

    public static void implAccelerator(NAR n, NAgent a) {
        TermGraph.ImplGraph tg = new TermGraph.ImplGraph();
        a.onFrame(new Consumer<NAgent>() {

            MutableValueGraph<Term, Float> s = null;

            float decay = 0.5f;
            float min = 0.01f;

            @Override
            public void accept(NAgent r) {
                if (s != null) {

                    List<EndpointPair<Term>> toRemove = new FasterList();
                    Iterator<EndpointPair<Term>> ii = s.edges().iterator();
                    while (ii.hasNext()) {
                        EndpointPair<Term> e = ii.next();
                        Term a = e.source();
                        Term b = e.target();
                        float next = this.s.edgeValue(a, b) * decay;
                        if (next < min) {
                            toRemove.add(e);
                        } else {
                            s.putEdgeValue(a, b, next);
                        }
                    }
                    toRemove.forEach(rr -> s.removeEdge(rr.source(), rr.target()));
                }
                s = tg.snapshot(s,
                        Iterables.concat(
                              Iterables.transform(a.actions, ActionConcept::term),
                            Lists.newArrayList(a.happy.term())
                        )
                        , n, n.time());

                Set<EndpointPair<Term>> ee = s.edges();

                if (!ee.isEmpty()) {

                    List<EndpointPair<Term>> ff = $.newArrayList();
                    ff.addAll(ee);
                    ff.sort((aa, bb) -> { //TODO faster
                        float av = s.edgeValue(aa.source(), aa.target());
                        float bv = s.edgeValue(bb.source(), bb.target());
                        int o = Float.compare(bv, av);
                        if (o == 0) {
                            return Integer.compare(aa.hashCode(), bb.hashCode());
                        }
                        return o;
                    });

                    float confMin = n.confMin.floatValue();

                    ///System.out.println(ee.size() + " total implication edges");

                    ff
                            //.subList(0, Math.min(ff.size(), 40))
                            .forEach(e -> {
                                Term subj = e.source();
                                Term pred = e.target();

//                                float activation = 0.5f;
//
//                                Concept csubj = n.concept(subj);
//                                if (csubj != null)
//                                    n.activate(csubj, activation);
//
//                                Concept cpred = n.concept(pred);
//                                if (cpred != null)
//                                    n.activate(cpred, activation);

                                float v = Math.min(1f, s.edgeValue(subj, pred));

                                float c = n.confDefault(GOAL) * v;
                                if (c > confMin) {
                                    if (pred.equals(a.happy)) {
                                        Task x = n.goal(subj, Tense.Present, 1f, c);
                                    } else if (pred.op() == NEG && pred.unneg().equals(a.happy)) {
                                        Task x = n.goal(subj, Tense.Present, 0f, c);
                                    }

                                    if (subj.equals(a.happy)) {
                                        Task x = n.goal(pred, Tense.Present, 1f, c);
                                    } else if (subj.op() == NEG && subj.unneg().equals(a.happy)) {
                                        Task x = n.goal(pred, Tense.Present, 0f, c);
                                    }

//                                    System.out.println(
//                                            Texts.n4(v) + "\t(" + subj + "==>" + pred + ")"
//
//                                    );
                                }
                            }

                    );
                }
            }
        });
    }


}


//    private static class InteractiveFirer extends FireConcepts.DirectConceptBagFocus {
//
//        private Premise premise;
//
//        public InteractiveFirer(NAR n) {
//            super(n, ((Default) n).newConceptBag(1024), ((Default) n).newPremiseBuilder());
//        }
//
//        final Set<Task> derived = new HashSet(1024);
//
//        @Override
//        protected synchronized void cycle() {
//
//            new PremiseMatrix(1, 1, new MutableIntRange(1,1)).accept(nar);
//
//            if (!derived.isEmpty()) {
//                System.out.println(premise);
//
//                List<Task> l = new FasterList(derived);
//                l.sort((a, b)->{
//                   int x = Float.compare(b.budget().pri(), a.pri());
//                   if (x == 0)
//                       return 1;
//                   else
//                       return x;
//                });
//
//                derived.clear();
//
//                for (Task x : l) {
//                    System.out.println("\t" + x);
//                }
//                try {
//                    System.in.read();
//                } catch (IOException e) {
//
//                }
//            }
//        }
//
//
//
//        @Override
//        public void accept(DerivedTask derivedTask) {
//            //nar.input(derivedTask);
//            premise = derivedTask.premise;
//            derived.add(derivedTask);
//        }
//    }
