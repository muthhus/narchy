package nars.control;

import jcog.Util;
import jcog.bag.Bag;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class ConceptFire extends UnaryTask<Concept> implements Termed {

    /**
     * rate at which ConceptFire forms premises
     */
    private static final int samplesMax = 8;
    private static final float priMinAbsolute = Pri.EPSILON * 2;
    private static final float momentum = 0.5f;

    static final ThreadLocal<Map<DerivedTask, DerivedTask>> buffers =
            ThreadLocal.withInitial(LinkedHashMap::new);

    static final int TASKLINKS_SAMPLED = samplesMax;
    static final int TERMLINKS_SAMPLED = samplesMax;

    public ConceptFire(Concept c, float pri) {
        super(c, pri);
    }


    @Override
    public ITask[] run(NAR nar) {
        float priBefore = this.pri;
        if (priBefore != priBefore || priBefore < priMinAbsolute)
            return null;

        final float minPri = priMinAbsolute;

        final Concept c = id;

        final Bag<Task, PriReference<Task>> tasklinks = c.tasklinks().commit();//.normalize(0.1f);
        if (tasklinks.isEmpty())
            return null;

        final Bag<Term, PriReference<Term>> termlinks = c.termlinks().commit();//.normalize(0.1f);
        nar.terms.commit(c); //index cache update


        List<PriReference<Task>> taskl = $.newArrayList();

        tasklinks.sample(TASKLINKS_SAMPLED, ((Consumer<PriReference<Task>>) taskl::add));
        if (taskl.isEmpty()) return null;

        List<PriReference<Term>> terml = $.newArrayList();
        termlinks.sample(TERMLINKS_SAMPLED, ((Consumer<PriReference<Term>>) terml::add));
        if (terml.isEmpty()) return null;

        @Nullable PriReference<Task> tasklink = null;
        @Nullable PriReference<Term> termlink = null;

        int samples = 0;
        int premises = 0;
        int derivations = 0;
        float cost = 0;
        int samplesMax = Math.min(this.samplesMax, terml.size() * taskl.size());

        Map<DerivedTask, DerivedTask> results = buffers.get();
        Consumer<DerivedTask> resultMerger = (nt) -> results.merge(nt, nt, (tt, pt) -> {
            if (pt == null) {
                priSub(nt.priElseZero());
                return nt;
            } else {
                float ptBefore = pt.priElseZero();
                pt.merge(nt);
                float ptAfter = pt.priElseZero();
                priSub(ptAfter - ptBefore);
                return pt;
            }
        });


        Random rng = nar.random();
        while (++samples < samplesMax && priElseZero() >= minPri) {
            tasklink = taskl.get(
                    Util.selectRoulette(taskl.size(), (i) -> taskl.get(i).priElseZero(), rng)
            );
            termlink = terml.get(
                    Util.selectRoulette(terml.size(), (i) -> terml.get(i).priElseZero(), rng)
            );

            //            if (tasklink == null || (rng.nextFloat() > taskLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
            //                tasklink = tasklinks.sample();
            //                if (tasklink == null)
            //                    break;
            //
            //                taskLinkPri = clamp(tasklinks.normalizeMinMax(tasklink.priElseZero()), taskMargin, 1f-taskMargin);
            //            }
            //
            //
            //            if (termlink == null || (rng.nextFloat() > termLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
            //                termlink = termlinks.sample();
            //                if (termlink == null)
            //                    break;
            //                termLinkPri = clamp(termlinks.normalizeMinMax(termlink.priElseZero()), termMargin, 1f-termMargin);
            //            }

//                if (termlink.get().op() == NEG)
//                    throw new RuntimeException("NEG termlink: " + termlink);

            Premise p = new Premise(tasklink, termlink, resultMerger);
            premises++;

            float thisPri = priElseZero();
            float pLimitFactor = thisPri / samplesMax * (1f - momentum);
            p.setPri(pLimitFactor);

            priSub(p.priElseZero()); //pay up-front

            p.run(nar);

//            float pAfter = p.priElseZero();
//            float ba = pBefore - pAfter;
//            if (ba >= Pri.EPSILON)
//                priSub(ba);

            //                if (result!=null) {
            //                    for (ITask x : result) {
            //                        results.merge(x, x, (pv, nv) -> {
            //                           pv.merge(nv);
            //                           return pv;
            //                        });
            //                    }
            //                    float cost = thisPri - p.priElseZero();
            //                    priSub(cost);
            //                }
        }

        derivations = results.size();

        //float priAfter = priElseZero();
        //cost = priBefore - priAfter;
        //System.out.println(this + " " + samples + "," + premises + "," + derivations + "," + cost);

        if (derivations > 0) {
            ITask[] a = results.values().toArray(new ITask[derivations]);
            results.clear();
            return a;
        } else
            return null;


//        int num = premises.size();
//        if (num > 0) {
//            ITask[] pp = premises.array();
//            float spend = this.pri * spendRate;
//            this.pri -= spend;
//
//            //divide priority among the premises
//            float subPri = spend / num;
//            for (int i = 0; i < num; i++) {
//                pp[i].setPri(subPri);
//            }
//
//            return pp;
//        } else {
//            return null;
//        }
    }

    @NotNull
    @Override
    public final Term term() {
        return id.term();
    }

}
