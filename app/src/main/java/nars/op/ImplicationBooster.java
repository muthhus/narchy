package nars.op;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static nars.Op.GOAL;
import static nars.Op.NEG;
import static nars.truth.TruthFunctions.w2c;

/**
 * Created by me on 4/30/17.
 */
public class ImplicationBooster extends DurService {

    private final TermGraph.ImplGraph tg;
    private final Iterable<Termed> seeds;

    @Deprecated float momentum = 0.5f;
    float min = Pri.EPSILON; //even though it's for truth

    MutableValueGraph<Term, Float> s = null;

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
            List<EndpointPair<Term>> toRemove = new FasterList<>();
            Iterator<EndpointPair<Term>> ii = s.edges().iterator();
            while (ii.hasNext()) {
                EndpointPair<Term> e = ii.next();
                Term a = e.source();
                Term b = e.target();
                float next = this.s.edgeValue(a, b).orElseGet(() -> {
                    toRemove.add(e);
                    return 0f;
                }) * momentum;
                if (next >= min) {
                    s.putEdgeValue(a, b, next);
                }
            }
            toRemove.forEach(rr -> s.removeEdge(rr.source(), rr.target()));


        }

        s = tg.snapshot(s, seeds, nar, targetTime);

        Set<EndpointPair<Term>> ee = s.edges();

//        if (ee.isEmpty()) {
//            return;
//        }

//        List<EndpointPair<Term>> ff = $.newArrayList(ee.size());
//        ff.addAll(ee);
//        ff.sort((aa, bb) -> { //TODO faster
//            float av = s.edgeValue(aa.source(), aa.target()).orElse(0f);
//            float bv = s.edgeValue(bb.source(), bb.target()).orElse(0f);
//            int o = Float.compare(bv, av);
//            if (o == 0) {
//                return Integer.compare(aa.hashCode(), bb.hashCode());
//            }
//            return o;
//        });


        System.out.println(ee.size() + " total implication edges");

        //int maxInputs = 40;
        Map<Term, TruthAccumulator> adjusts = new HashMap();

        //.subList(0, Math.min(ff.size(), maxInputs))
        for (EndpointPair<Term> e : s.edges()) {


            Term subj = e.source();
            Term pred = e.target();


            Term tt = null;
            final float freq;
            float w = s.edgeValue(subj, pred).orElse(0f);
            if (w < Pri.EPSILON)
                continue;

            float c = w2c(w);
            if (c > confMin) {
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
                } else {
                    continue;
                }

                recurse = s.predecessors(subj);

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


                add(adjusts, tt, freq, c);

                if (recurse != null) {

                    recurse.forEach(rrr -> {
                        float cc = c * w2c(s.edgeValue(rrr, subj).orElse(0f));
                        //System.out.println(e + " " + rrr + " " );
                        add(adjusts, rrr, freq, cc);
                    });
                }
                //                                    System.out.println(
                //                                            Texts.n4(v) + "\t(" + subj + "==>" + pred + ")"
                //
                //                                    );
            }
        }


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
