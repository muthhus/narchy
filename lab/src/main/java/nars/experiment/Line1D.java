package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import jcog.list.FasterList;
import jcog.pri.Priority;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.test.agent.Line1DSimplest;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.util.graph.TermGraph;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static nars.NAgentX.runRT;
import static nars.Op.GOAL;
import static nars.Op.NEG;
import static nars.truth.TruthFunctions.w2c;

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

            n.termVolumeMax.setValue(32);
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
                        0.5f * (Math.sin(n.time() / 200f) + 1f)
                        //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                        //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                );
            });

            //a.curiosityProb.setValue(0f);

            return a;

        }, 20);

    }

    public static void implAccelerator(NAR n, NAgent a) {
        TermGraph.ImplGraph tg = new TermGraph.ImplGraph();
        //a.onFrame(
        a.nar.onCycle(
                new Consumer() {

            MutableValueGraph<Term, Float> s = null;

            float momentum = 0.5f;
            float min = 0.001f;

            @Override
            public void accept(Object r) {
                if (s != null) {

                    List<EndpointPair<Term>> toRemove = new FasterList();
                    Iterator<EndpointPair<Term>> ii = s.edges().iterator();
                    while (ii.hasNext()) {
                        EndpointPair<Term> e = ii.next();
                        Term a = e.source();
                        Term b = e.target();
                        float next = this.s.edgeValue(a, b) * momentum;
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
                        , n, n.time() + n.dur()/2);

                Set<EndpointPair<Term>> ee = s.edges();

                if (ee.isEmpty()) {
                    return;
                }

                List<EndpointPair<Term>> ff = $.newArrayList(ee.size());
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
                long now = n.time();
                int dur = n.dur();

                System.out.println(ee.size() + " total implication edges");

                //int maxInputs = 40;
                Map<Term,TruthAccumulator> adjusts = new HashMap();

                //.subList(0, Math.min(ff.size(), maxInputs))
                for (EndpointPair<Term> e : ff) {


                    Term subj = e.source();
                    Term pred = e.target();


                    Term tt = null;
                    final float freq;
                    float c = w2c(s.edgeValue(subj, pred));
                    Set<Term> recurse = null;
                    if (c > confMin) {

                        if (pred.equals(a.happy)) {
                            tt = subj;
                            freq = 1f;
                            recurse = s.predecessors(subj);
                        } else if (pred.op() == NEG && pred.unneg().equals(a.happy)) {
                            tt = subj;
                            freq = 0f;
                            recurse = s.predecessors(subj);
//                        } else if (subj.equals(a.happy)) {
//                            tt = pred;
//                            freq = 1f;
//                            //recurse = s.successors(pred);
//                        } else if (subj.op() == NEG && subj.unneg().equals(a.happy)) {
//                            tt = pred;
//                            freq = 0f;
//                            //recurse = s.successors(pred);
                        } else {
                            freq = Float.NaN;
                        }

                        if (tt != null) {

                            float activation = w2c(s.edgeValue(subj, pred));
                            if (activation >= Priority.EPSILON_DEFAULT) {
                                Concept csubj = n.concept(subj);
                                if (csubj != null)
                                    n.activate(csubj, activation);

                                Concept cpred = n.concept(pred);
                                if (cpred != null)
                                    n.activate(cpred, activation);
                            }

                            add(adjusts, tt, freq, c);

                            if (recurse!=null) {

                                recurse.forEach(rrr -> {
                                    float c2 = w2c(s.edgeValue(rrr, subj));
                                    float cc = c * c2;
                                    if (cc > confMin) {
                                        //System.out.println(e + " " + rrr + " " );
                                        add(adjusts, rrr, freq, cc);
                                    }

                                });
                            }
                        }
//                                    System.out.println(
//                                            Texts.n4(v) + "\t(" + subj + "==>" + pred + ")"
//
//                                    );
                    }
                }

                adjusts.forEach((tt,a)->{
                    @Nullable Truth uu = a.commitSum();
                    float c = uu.conf();
                    if (uu!=null && c > confMin) {
                        Task x = n.goal(n.priorityDefault(GOAL), (Compound)tt, now + dur / 2, uu.freq(), c);
                        System.out.println("\t" + x);
                    }
                });
            }

                    public void add(Map<Term, TruthAccumulator> adjusts, Term tt, float freq, float c) {
                        adjusts.compute(tt, (ttt, p) -> {
                            if (p==null)
                                p = new TruthAccumulator();
                            p.add($.t(freq, c));
                            return p;
                        });
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
