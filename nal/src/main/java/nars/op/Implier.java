package nars.op;

import jcog.data.graph.AdjGraph;
import jcog.list.FasterList;
import jcog.pri.Prioritized;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.control.CauseChannel;
import nars.control.DurService;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.var.Variable;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.truth.func.GoalFunction;
import nars.truth.func.TruthOperator;
import nars.util.graph.TermGraph;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.truth.TruthFunctions.w2c;


/**
 * causal implication booster / compiler
 */
public class Implier extends DurService {

    private final TermGraph.ImplGraph tg;
    private final Iterable<Term> seeds;
    private final NAR nar;
    private final CauseChannel<Task> in;

    float min = Prioritized.EPSILON; //even though it's for truth
    Map<Term, TruthAccumulator> goalTruth = new HashMap();

    AdjGraph<Term, Term> impl = null;

    private float relativeTargetDur = +1f;

    /**
     * truth cache
     */
    private HashMap<Term, Truth> desire = new HashMap();
    /**
     * truth cache
     */
    private HashMap<Term, Task> belief = new HashMap();


    final static TruthOperator ded = GoalFunction.get($.the("GoductionRecursivePB"));
    final static TruthOperator ind = GoalFunction.get($.the("InductionRecursivePB"));
    private long now;

    public Implier(NAR n, Term... seeds) {
        this(n, List.of(seeds));
    }


    public Implier(NAR n, Iterable<Term> seeds) {
        super(n, 1f);

        this.nar = n;
        this.seeds = seeds;
        this.in = n.newCauseChannel(this);
        this.tg = new TermGraph.ImplGraph() {
            @Override
            protected boolean acceptTerm(Term p) {
                return !(p instanceof Variable);// && !p.isTemporal();
            }
        };
    }

    @Override
    protected void run(NAR nar) {




        desire.clear();
        belief.clear();
        goalTruth.clear();

        if (impl != null && impl.edgeCount() > 256) { //HACK
//            System.err.print("saved impl graph to file");
//            try {
//                impl.writeGML(new PrintStream(new FileOutputStream("/tmp/x.gml")));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
            impl = null; //reset
        }

        impl = tg.snapshot(impl, seeds, nar);
        int implCount = impl.edgeCount();

        if (implCount == 0)
            return;

        float confMin = nar.confMin.floatValue();
        float confSubMin = confMin / implCount;

        int dur = nar.dur();
        now = nar.time();

        //System.out.println(impl);

        impl.each((subj, pred, impl) -> {


            Task SGimpl = belief(impl);
            if (SGimpl == null)
                return;

            float implConf = w2c(SGimpl.evi(this.now, dur));
            if (implConf < confSubMin)
                return;

            int implDT = SGimpl.dt();
            if (implDT == DTERNAL)
                implDT = 0;


            float f = SGimpl.freq();


            {
                //G, (S ==> G) |- S  (Goal:DeductionRecursivePB)
                Truth Pg = desire(pred, this.now + implDT); //the desire at the predicate time
                if (Pg == null)
                    return;

                Truth Sg = ded.apply(Pg, $.t(f, implConf), nar, confSubMin);

                if (Sg != null) {
                    goal(goalTruth, subj, Sg);
                }
            }
            //experimental:
//            {
//                //G, (G ==> P) |- P (Goal:InductionRecursivePB)
//                //G, ((--,G) ==> P) |- P (Goal:InductionRecursivePBN)
//
//                //HACK only immediate future otherwise it needs scheduled further
//                if (implDT >= 0 && implDT <= dur/2) {
//
//                    Truth Ps = desire(subj, nowStart, nowEnd); //subj desire now
//                    if (Ps == null)
//                        return;
//
//                    float implFreq = f;
//
//
//
//                    if (Ps.isNegative()) {
//                        subj = subj.neg();
//                        Ps = Ps.neg();
//                    }
//
//                    //TODO invert g and choose indRec/indRecN
//                    Truth Pg = indRec.apply(Ps, $.t(implFreq, implConf), nar, confSubMin);
//                    if (Pg != null) {
//                        goal(goalTruth, pred, Pg);
//                    }
//                }
//            }

        });


//            List<IntHashSet> ws = new GraphMeter().weakly(s);
//            ws.forEach(x -> {
//                if (!x.isEmpty()) { //HACK
//                    System.out.println( x.collect(i -> s.node(i)) );
//                }
//            });

        goalTruth.forEach((t, a) -> {
            @Nullable Truth uu = a.commitSum();
            if (uu != null) {
                float c = uu.conf();
                if (c >= confMin) {
                    NALTask y = new NALTask(t, GOAL, uu, now, now, now /* + dur */,
                            nar.time.nextInputStamp());
                    y.pri(nar.priDefault(GOAL));
//                        if (Param.DEBUG)
//                            y.log("")
                    in.input(y);
                    System.err.println("\t" + y);
                }
            }
        });

//        if (s!=null)
//            System.out.println(s.toString());

    }

    private Truth desire(Term x, long from) {
        return nar.goalTruth(x, from);
    }

    private Truth desire(Term x) {
        return desire.computeIfAbsent(x, (xx) -> desire(xx, now));
    }

    private Task belief(Term x) {
        return belief.computeIfAbsent(x, (xx) -> nar.belief(xx, now));
    }

    public void goal(Map<Term, TruthAccumulator> goals, Term tt, Truth g) {

        if (tt.op() == NEG) {
            tt = tt.unneg();
            g = g.neg();
        }

        //recursively divide the desire among the conjunction events, emulating (not necessarily exactly) StructuralDeduction's
        if (tt.op() == CONJ) {
            FasterList<ObjectLongPair<Term>> e = tt.events();
            if (e.size() > 1) {
                float eSub = g.evi() / e.size();
                float cSub = w2c(eSub);
                if (cSub >= nar.confMin.floatValue()) {
                    Truth gSub = $.t(g.freq(), cSub);
                    for (ObjectLongPair<Term> ee : e) {
                        goal(goals, ee.getOne(), gSub);
                    }
                }
                return;
            }
        }

        goals.computeIfAbsent(tt, (ttt) -> {
            return new TruthAccumulator();
        }).add(g);
    }
}
