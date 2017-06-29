package nars.control;

import jcog.Util;
import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
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
     * rate at which ConceptFire forms premises and derives
     */
    private static final int maxSamples = 2;

    static final int TASKLINKS_SAMPLED = maxSamples * 1;
    static final int TERMLINKS_SAMPLED = maxSamples * 2;

    //private static final float priMinAbsolute = Pri.EPSILON * 1;
    //private static final float momentum = 0.75f;

    static final ThreadLocal<Map<DerivedTask, DerivedTask>> buffers =
            ThreadLocal.withInitial(LinkedHashMap::new);




    public ConceptFire(Concept c, float pri) {
        super(c, pri);
    }


    @Override
    public ITask[] run(NAR nar) {

        //nar.emotion.count("ConceptFire_run_attempt");

//        float priBefore = this.pri;
//        if (priBefore != priBefore) {
//            nar.emotion.count("ConceptFire_run_but_deleted");
//            return null;
//        }
//        if (priBefore < priMinAbsolute) {
//            nar.emotion.count("ConceptFire_run_but_depleted");
//            return null;
//        }
//
//
//        final float minPri = priMinAbsolute;

        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks().commit();//.normalize(0.1f);
        if (tasklinks.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks");
            return null;
        }
        List<PriReference<Task>> taskl = $.newArrayList();
        tasklinks.sample(TASKLINKS_SAMPLED, ((Consumer<PriReference<Task>>) taskl::add));
        if (taskl.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks_selected");
            return null;
        }

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks().commit();//.normalize(0.1f);
        List<PriReference<Term>> terml = $.newArrayList();
        termlinks.sample(TERMLINKS_SAMPLED, ((Consumer<PriReference<Term>>) terml::add));
        if (terml.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_termlinks_selected");
            return null;
        }

        //nar.emotion.count("ConceptFire_run");

        nar.terms.commit(id); //index cache update

        @Nullable PriReference<Task> tasklink = null;
        @Nullable PriReference<Term> termlink = null;

        //int samples = 0;
        int premises = 0;
        int derivations = 0;
        float cost = 0;
                //Math.min(this.samplesMax, terml.size() * taskl.size());

        Map<DerivedTask, DerivedTask> results = buffers.get();
        Consumer<DerivedTask> x = (nt) -> results.merge(nt, nt, (tt, pt) -> {
            if (pt == null) {
                //priSub(nt.priElseZero());
                return nt;
            } else {
                //float ptBefore = pt.priElseZero();
                pt.merge(nt);
                //float ptAfter = pt.priElseZero();
                //priSub(ptAfter - ptBefore);
                return pt;
            }
        });
        //float pLimitFactor = priElseZero() * (1f - momentum) / samplesMax;

        int ttlPerPremise =
                //Param.UnificationTTLMax
                (int)Math.ceil((0.75f * priElseZero() + 0.25f) * Param.UnificationTTLMax)
        ;

        int maxTTL = maxSamples * ttlPerPremise;
        int ttl = maxTTL;

        int termlSize = terml.size();
        if (termlSize == 0)
            return null;
        float[] termlinkPri = new float[termlSize];
        for (int i = 0; i < termlSize; i++)
            termlinkPri[i] = terml.get(i).priElseZero();
        int tasklSize = taskl.size();
        if (tasklSize == 0)
            return null;
        float[] tasklinkPri = new float[tasklSize];
        for (int i = 0; i < tasklSize; i++)
            tasklinkPri[i] = taskl.get(i).priElseZero();

        Random rng = nar.random();
        while (ttl > 0 /*samples++ < samplesMax*/) {

            int tasklSelected = Util.decideRoulette(tasklSize, (i) -> tasklinkPri[i], rng);
            tasklink = taskl.get( tasklSelected );

            int termlSelected = Util.decideRoulette(termlSize, (i) -> termlinkPri[i], rng);
            termlink = terml.get( termlSelected );

            Premise p = new Premise(tasklink, termlink, x);
            premises++;


            int ttlUsed = p.run(nar, ttlPerPremise); //inline
//            if (ttlUsed <= 0) {
//                //failure penalty
//                tasklinkPri[tasklSelected] *= 0.9f;
//                termlinkPri[termlSelected] *= 0.9f;
//            }
            ttl -= Math.max(ttlUsed, ttlPerPremise/2);

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
