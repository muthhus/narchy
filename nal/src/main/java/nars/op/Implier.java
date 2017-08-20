package nars.op;

import jcog.data.graph.AdjGraph;
import jcog.pri.Pri;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.control.DurService;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.util.graph.TermGraph;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static nars.Op.GOAL;
import static nars.Op.NEG;
import static nars.truth.TruthFunctions.w2c;


/** causal implication booster / compiler */
public class Implier extends DurService {

    private final TermGraph.ImplGraph tg;
    private final Iterable<Term> seeds;

    @Deprecated float momentum = 0f;
    float min = Pri.EPSILON; //even though it's for truth
    Map<Term, TruthAccumulator> goals = new HashMap();

    AdjGraph<Term, Float> graph = null;

    private float relativeTargetDur = +1f;


    public Implier(NAR n, Term... seeds) {
        this(n, List.of(seeds));
    }

    public Implier(NAR n, Iterable<Term> seeds) {
        super(n, 1f);

        this.seeds = seeds;
        this.tg = new TermGraph.ImplGraph() {
            @Override
            protected boolean acceptTerm(Term p) {
                return !p.isTemporal();
            }
        };
    }

    @Override
    protected void runDur(NAR nar) {

        float confMin = nar.confMin.floatValue();
        long now = nar.time();
        int dur = nar.dur();
        long targetTime = Math.round(now + relativeTargetDur * dur);

        if (graph != null) {
            //prune or forget
            if (momentum > 0) {
                graph = graph.compact((a, b, prev) -> {
                    float next = prev * momentum;
                    if (next >= min) {
                        //s.setEdge(a, b, next);
                        return next;
                    }
                    return null;
                });
            } else {
                graph = null;
            }
        }

        graph = tg.snapshot(graph, seeds, nar, targetTime);

        goals.clear();

        graph.each((subj, pred, w) -> {

            if (w < Pri.EPSILON)
                return;


            float c = w2c(w);
            if (c > confMin) {
                Term tt = null;
                Set<Term> recurse = null;


                for (Termed target : seeds) {
                    final float freq;

                    if (pred.equals(target)) {
                        tt = subj;
                        freq = 1f;
                    } else if (pred.op() == NEG && pred.unneg().equals(target)) {
                        tt = subj;
                        freq = 0f;
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

                    if (freq==freq)
                        goal(goals, tt, freq, c);
                }



                //float activation = w2c(s.edgeValue(subj, pred));
//                                    if (activation >= Priority.EPSILON) {
//                                        Concept csubj = n.concept(subj);
//                                        if (csubj != null)
//                                            n.activate(csubj, activation);
//
//                                        Concept cpred = n.concept(pred);
//                                        if (cpred != null)
//                                            n.activate(cpred, activation);
//                                    }




//                recurse = s.predecessors(subj);
//                if (recurse != null) {
//
//                    recurse.forEach(rrr -> {
//                        float cc = c * w2c(s.edge(rrr, subj, 0f));
//                        //System.out.println(e + " " + rrr + " " );
//                        add(adjusts, rrr, freq, cc);
//                    });
//                }
                //                                    System.out.println(
                //                                            Texts.n4(v) + "\t(" + subj + "==>" + pred + ")"
                //
                //                                    );
            }
        });


//            if (nar.random().nextInt(25) == 0) {
//                System.err.println("saving graph");
//                try {
//                    s.writeGML(new PrintStream(new FileOutputStream("/tmp/x.gml")));
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }

        if (!goals.isEmpty()) {

//            List<IntHashSet> ws = new GraphMeter().weakly(s);
//            ws.forEach(x -> {
//                if (!x.isEmpty()) { //HACK
//                    System.out.println( x.collect(i -> s.node(i)) );
//                }
//            });

            goals.forEach((tt, a) -> {
                @Nullable Truth uu = a.commitSum();
                if (uu != null) {
                    float c = uu.conf();
                    if (c >= confMin) {
                        Task x = nar.goal(nar.priorityDefault(GOAL), tt,
                                now, targetTime,
                                uu.freq(), c);
                        System.out.println("\t" + x);
                    }
                }
            });
        }

//        if (s!=null)
//            System.out.println(s.toString());

    }

    public void goal(Map<Term, TruthAccumulator> goals, Term tt, float freq, float c) {
        if (tt.op()==NEG) {
            tt = tt.unneg();
            freq = 1 - freq;
        }

        @Nullable Truth dt = $.t(freq, c);

        goals.compute(tt, (ttt, p) -> {
            if (p == null)
                p = new TruthAccumulator();
            p.add(dt);
            return p;
        });
    }
}
