package nars.op;

import jcog.data.graph.AdjGraph;
import jcog.pri.Pri;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nars.Op.GOAL;
import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;


/**
 * causal implication booster / compiler
 */
public class Implier extends DurService {

    private final TermGraph.ImplGraph tg;
    private final Iterable<Term> seeds;
    private final NAR nar;
    private final CauseChannel<Task> in;

    float min = Pri.EPSILON; //even though it's for truth
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

    private long next;
    private long now;

    final static TruthOperator dedRec = GoalFunction.get($.the("DeductionRecursivePB"));

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
                return !(p instanceof Variable) && !p.isTemporal();
            }
        };
    }

    @Override
    protected void runDur(NAR nar) {

        int dur = nar.dur();
        now = nar.time();
        next = (now + (long) (relativeTargetDur * dur));

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

        impl = tg.snapshot(impl, seeds, nar, next);
        int implCount = impl.edgeCount();

        if (implCount == 0)
            return;

        float confMin = nar.confMin.floatValue();
        float confSubMin = confMin / implCount;


        //System.out.println(impl);

        impl.each((subj, pred, impl) -> {


            Task SGimpl = belief(impl);
            if (SGimpl == null)
                return;

            //G, (S ==> G) |- S  (Goal:DeductionRecursivePB)

            float SGimplTruth = SGimpl.conf(now, dur);
            if (SGimplTruth < confSubMin)
                return;

            int dt = SGimpl.dt();
            if (dt == DTERNAL)
                dt = 0;

            Truth Gg = desire(pred, +dt); //the desire at the predicate time
            if (Gg == null)
                return;

            float f = SGimpl.freq();
            if (subj.op() == NEG) {
                subj = subj.unneg();
                f = 1 - f;
            }

            Truth Sg = dedRec.apply(Gg, $.t(f, SGimplTruth), nar, confSubMin);

            if (Sg != null) {
                goal(goalTruth, subj, Sg);
            }

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
                    NALTask y = new NALTask(t, GOAL, uu, now, now, next, nar.time.nextInputStamp());
                    y.pri(nar.priorityDefault(GOAL));
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

    private Truth desire(Term x, long when) {
        return nar.goalTruth(x, when);
    }

    private Truth desire(Term x) {
        return desire.computeIfAbsent(x, (xx) -> desire(xx, next));
    }

    private Task belief(Term x) {
        return belief.computeIfAbsent(x, (xx) -> nar.belief(xx, now, now));
    }

    public void goal(Map<Term, TruthAccumulator> goals, Term tt, Truth g) {
        goals.compute(tt, (ttt, p) -> {
            if (p == null)
                p = new TruthAccumulator();
            p.add(g);
            return p;
        });
    }
}
