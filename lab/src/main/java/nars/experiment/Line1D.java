package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import jcog.Texts;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.term.Term;
import nars.test.agent.Line1DSimplest;
import nars.util.graph.TermGraph;

import java.util.List;
import java.util.Set;

import static nars.NAgentX.runRT;

/**
 * Created by me on 3/15/17.
 */
public class Line1D {

    public static void main(String[] args) {
        //Param.DEBUG = true;


        NAR nar = runRT((NAR n) -> {

            //n.setControl(new InteractiveFirer(n));

            //n.log();

//            n.onTask(x -> {
//                if (x.isGoal() && !x.isInput()) {
//                    System.err.println(x.proof());
//                }
//            });

            n.termVolumeMax.setValue(20);
            n.beliefConfidence(0.5f);
            n.goalConfidence(0.5f);


            Line1DSimplest a = new Line1DSimplest(n);

            //n.log();

            implAccelerator(n, a);


            //TEST CHEAT:
            //n.believe((Compound) $.impl($.inh($.diffe($.the("i"), $.the("o")), a.id), 1, a.out.term()), 1f, 0.99f);
            //n.believe((Compound) $.impl($.inh($.diffe($.the("o"), $.the("i")), a.id), 1, a.out.term()), 0f, 0.99f);


            n.onCycle(() -> {
                a.i.setValue(
                        0.5f * (Math.sin(n.time() / 80f) + 1f)
                        //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                        //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                );
            });

            //a.curiosityProb.setValue(0f);

            return a;

        }, 20, 1, -1);

    }

    public static void implAccelerator(NAR n, NAgent a) {
        TermGraph.ImplGraph tg = new TermGraph.ImplGraph(n);
        n.onCycle(r -> {
            MutableValueGraph<Term, Float> s = tg.snapshot(
                    Iterables.concat(
                            Iterables.transform(a.actions, ActionConcept::term),
                            Lists.newArrayList(a.happy.term())
                    ),
                    n);
            Set<EndpointPair<Term>> ee = s.edges();

            if (!ee.isEmpty()) {

                List<EndpointPair<Term>> ff = $.newArrayList();
                ff.addAll(ee);
                ff.sort((aa, bb) -> { //TODO faster
                    float av = s.edgeValue(aa.nodeU(), aa.nodeV());
                    av = Math.max(av - 0.5f, 0.5f - av);
                    float bv = s.edgeValue(bb.nodeU(), bb.nodeV());
                    bv = Math.max(bv - 0.5f, 0.5f - bv);
                    int o = Float.compare(bv, av);
                    if (o == 0) {
                        return Integer.compare(aa.hashCode(), bb.hashCode());
                    }
                    return o;
                });


                //System.out.println(ee.size() + " total implication edges");
                ff.subList(0, Math.min(ff.size(), 10)).forEach(e -> {
                            Term subj = e.nodeU();
                            Term pred = e.nodeV();

                            Concept csubj = n.concept(subj);
                            if (csubj != null)
                                n.activate(csubj, 0.1f);

                            Concept cpred = n.concept(pred);
                            if (cpred != null)
                                n.activate(cpred, 0.1f);

//                            System.out.println(
//                                    Texts.n4(s.edgeValue(subj, pred)) + "\t(" + e.source() + "==>" + e.target() + ")"
//                            );
                        }

                );
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
