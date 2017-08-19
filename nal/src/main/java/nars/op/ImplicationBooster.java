package nars.op;

import com.google.common.graph.EndpointPair;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.GraphMeter;
import jcog.list.FasterList;
import jcog.pri.Pri;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.control.DurService;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.util.graph.TermGraph;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import static nars.Op.GOAL;
import static nars.Op.NEG;
import static nars.truth.TruthFunctions.w2c;


public class ImplicationBooster extends DurService {

    private final TermGraph.ImplGraph tg;
    private final Iterable<Termed> seeds;

    @Deprecated float momentum = 0.98f;
    float min = Pri.EPSILON; //even though it's for truth
    Map<Term, TruthAccumulator> adjusts = new HashMap();

    AdjGraph<Term, Float> s = null;

    /**
     * TODO support multiple targets, or make each controlled from a separate class that uses the data collected here
     */
    private Term target;

    private float relativeTargetDur = +1f;


    public ImplicationBooster(NAgent a, Iterable<Termed> seeds, Term target) {
        super(a.nar, 1f);

        this.seeds = seeds;
        this.tg = new TermGraph.ImplGraph() {
            @Override
            protected boolean acceptTerm(Term p) {
                return !p.isTemporal();
            }
        };
        this.target = target;
    }

    @Override
    protected void runDur(NAR nar) {

        float confMin = nar.confMin.floatValue();
        long now = nar.time();
        int dur = nar.dur();
        long targetTime = Math.round(now + relativeTargetDur * dur);

        if (s != null) {
            //prune or forget
            s = s.compact((a, b, prev)-> {
                float next = prev * momentum;
                if (next >= min) {
                    //s.setEdge(a, b, next);
                    return next;
                }
                return null;
            });
        }

        s = tg.snapshot(s, seeds, nar, targetTime);

        adjusts.clear();

        s.each((subj, pred, w) -> {

            if (w < Pri.EPSILON)
                return;

            final float freq;

            float c = w2c(w);
            if (c > confMin) {
                Term tt = null;
                Set<Term> recurse = null;


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
                    add(adjusts, tt, freq, c);
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

        if (!adjusts.isEmpty()) {

//            List<IntHashSet> ws = new GraphMeter().weakly(s);
//            ws.forEach(x -> {
//                if (!x.isEmpty()) { //HACK
//                    System.out.println( x.collect(i -> s.node(i)) );
//                }
//            });

            adjusts.forEach((tt, a) -> {
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

    public void add(Map<Term, TruthAccumulator> adjusts, Term tt, float freq, float c) {
        if (tt.op()==NEG) {
            tt = tt.unneg();
            freq = 1 - freq;
        }

        @Nullable Truth dt = $.t(freq, c);

        adjusts.compute(tt, (ttt, p) -> {
            if (p == null)
                p = new TruthAccumulator();
            p.add(dt);
            return p;
        });
    }
}
