package nars.control;

import jcog.bag.Bag;
import jcog.decide.DecideRoulette;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.conceptualize.state.ConceptState;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.Math.*;
import static nars.Param.UnificationTTLMax;

public class ConceptFire extends UnaryTask<Concept> implements Termed {

    /**
     * rate at which ConceptFire forms premises and derives
     */
    private static final int maxSamples = 1;

    static final int TASKLINKS_SAMPLED = maxSamples * 2;
    static final int TERMLINKS_SAMPLED = maxSamples * 2;
    private Concept[] templateConcepts;
    private int templateConceptsCount;

    //private static final float priMinAbsolute = Pri.EPSILON * 1;
    //private static final float momentum = 0.75f;

//    static final ThreadLocal<Map<DerivedTask, DerivedTask>> buffers =
//            ThreadLocal.withInitial(LinkedHashMap::new);



    public ConceptFire(Concept c, float pri) {
        super(c, pri);
        assert (c.isNormalized()) :
                c + " not normalized";
    }



    final static FloatFunction<? super PriReference> linearPri = (p) -> {
        return Math.max(p.priElseZero(), Pri.EPSILON);
    };
    final static FloatFunction<? super PriReference> softMaxPri = (p) -> {
        return (float) exp(p.priElseZero() * 3 /* / temperature */);
    };

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
        DecideRoulette<PriReference<Task>> taskl = new DecideRoulette(softMaxPri);
        tasklinks.sample(TASKLINKS_SAMPLED, ((Consumer<PriReference<Task>>) taskl::add));
        if (taskl.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks_selected");
            return null;
        }

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks().commit();//.normalize(0.1f);
        DecideRoulette<PriReference<Term>> terml = new DecideRoulette(softMaxPri);
        termlinks.sample(TERMLINKS_SAMPLED, ((Consumer<PriReference<Term>>) terml::add));
        if (terml.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_termlinks_selected");
            //return null;

            //continue below, where termlinks may be added via a tasklink fire
        }

        //nar.emotion.count("ConceptFire_run");


        @Nullable PriReference<Task> tasklink = null;
        @Nullable PriReference<Term> termlink = null;

        //int samples = 0;
        int premises = 0;
        int derivations = 0;
        float cost = 0;
        //Math.min(this.samplesMax, terml.size() * taskl.size());

//        Map<DerivedTask, DerivedTask> results = buffers.get();
//        Consumer<DerivedTask> x = (nt) -> results.merge(nt, nt, (prev, NT) -> {
////            if (pt == null) {
////                //priSub(nt.priElseZero());
////                return nt;
////            } else {
////                if (tt!=nt)
//                    //pt.merge(nt);
//
//            prev.priMax(NT.priElseZero());
//            return prev;
//                //float ptBefore = pt.priElseZero();
//
//                //float ptAfter = pt.priElseZero();
//                //priSub(ptAfter - ptBefore);
////                return pt;
////            }
//        });


        //float pLimitFactor = priElseZero() * (1f - momentum) / samplesMax;

        int ttlPerPremise = UnificationTTLMax;
        int maxTTL = (int) ceil(max(
                1 * ttlPerPremise,
                priElseZero() * maxSamples * ttlPerPremise
        ));
        int ttl = maxTTL;




        if (templateConcepts==null) {
            TermContainer ctpl = id.templates();
            if (ctpl != null) {
                Set<Concept> templateConcepts = new UnifiedSet(id.term().size());
                ctpl.forEach(x -> {
                    Concept c = nar.conceptualize(x);
                    if (c != null)
                        templateConcepts.add(c);
                });
                this.templateConcepts = templateConcepts.toArray(new Concept[templateConceptsCount = templateConcepts.size()]);
            } else {
                templateConcepts = Concept.EmptyArray;
                templateConceptsCount = 0;
            }
        }

        float totalActivation = 0;//tasklink activation

        Random rng = nar.random();
        while (ttl > 0 /*samples++ < samplesMax*/) {

            int tasklSelected = taskl.decideWhich(rng);
            tasklink = taskl.get(tasklSelected);

            @Nullable PriReference<Task> tll = tasklink;
            Task txx = tll.get();
            if (txx == null) {
                ttl -= UnificationTTLMax / 2;
                continue;
            }

            float tfa = txx.priElseZero();
            if (tfa < Pri.EPSILON) {
                ttl -= UnificationTTLMax / 2;
                continue;
            }

            totalActivation += tfa;
            if (templateConceptsCount > 0) {
                float tfaEach = tfa / templateConceptsCount;
                if (tfaEach >= Pri.EPSILON) {
                    for (Concept c : templateConcepts)
                        c.tasklinks().putAsync(new PLinkUntilDeleted(txx, tfaEach));
                }
            }


            if (terml.isEmpty())
                break;
            int termlSelected = terml.decideWhich(rng);
            termlink = terml.get(termlSelected);


            int ttlUsed = run(nar, tasklink, termlink, nar::input, ttlPerPremise); //inline
            premises++;
//            if (ttlUsed <= 0) {
//                //failure penalty
//                tasklinkPri[tasklSelected] *= 0.9f;
//                termlinkPri[termlSelected] *= 0.9f;
//            }
            ttl -= max(ttlUsed, UnificationTTLMax / 2);

        }

        if (templateConceptsCount > 0) {
            float eachActivation = totalActivation / templateConceptsCount;
            if (eachActivation >= Pri.EPSILON) {
                for (Concept c : templateConcepts) {
                    float momentum = 0.5f;
                    id.termlinks().putAsync(new PLink(c, eachActivation * (1f - momentum)));
                    c.termlinks().putAsync(new PLink(id, eachActivation * momentum));
                    nar.input(new ConceptFire(c, eachActivation));
                }
                //priSub(totalActivation);
            }
        }

        nar.terms.commit(id); //index cache update

//        derivations = results.size();

        //float priAfter = priElseZero();
        //cost = priBefore - priAfter;
        //System.out.println(this + " " + samples + "," + premises + "," + derivations + "," + cost);

//        if (derivations > 0) {
//            ITask[] a = results.values().toArray(new ITask[derivations]);
//            results.clear();
//            return a;
//        } else
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

    protected int run(NAR nar, @Nullable PriReference<Task> tasklink, @Nullable PriReference<Term> termlink, Consumer<DerivedTask> x, int ttlPerPremise) {
        Premise p = new Premise(tasklink, termlink, x);
        return p.run(nar, ttlPerPremise);
    }

    @NotNull
    @Override
    public final Term term() {
        return id.term();
    }

}
