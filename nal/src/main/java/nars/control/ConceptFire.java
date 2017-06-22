package nars.control;

import jcog.Util;
import jcog.bag.Bag;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static jcog.Util.clamp;
import static nars.Op.NEG;

public class ConceptFire extends UnaryTask<Concept> implements Termed {

    /** rate at which ConceptFire forms premises */
    private static final int sampleLimit = 8;
    private static final float priMinAbsolute = Pri.EPSILON * 4;
    private static final float momentum = 0.1f;


    public ConceptFire(Concept c, float pri) {
        super(c, pri);
    }


    @Override
    public ITask[] run(NAR nar) {
        float pri = this.pri;
        if (pri!=pri || pri < priMinAbsolute)
            return null;

        final float minPri = Math.max( pri * momentum, priMinAbsolute);

        final Concept c = id;

        final Bag<Task, PriReference<Task>> tasklinks = c.tasklinks().commit();//.normalize(0.1f);
        if (tasklinks.isEmpty())
            return null;

        final Bag<Term, PriReference<Term>> termlinks = c.termlinks().commit();//.normalize(0.1f);
        nar.terms.commit(c); //index cache update


        List<PriReference<Task>> taskl = $.newArrayList();
        List<PriReference<Term>> terml = $.newArrayList();
        tasklinks.sample(8, ((Consumer<PriReference<Task>>) taskl::add));
        termlinks.sample(8, ((Consumer<PriReference<Term>>) terml::add));

        @Nullable PriReference<Task> tasklink = null;
        @Nullable PriReference<Term> termlink = null;

        int ttl = sampleLimit;

        Random rng = nar.random();
        while (ttl-- > 0 && pri >= minPri) {
            tasklink = taskl.get(
                Util.selectRoulette(taskl.size(), (i)->taskl.get(i).priElseZero(), rng)
            );
            termlink = terml.get(
                Util.selectRoulette(terml.size(), (i)->terml.get(i).priElseZero(), rng)
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

            if (termlink.get().op()==NEG)
                throw new RuntimeException("NEG termlink: " + termlink);

            Premise p = new Premise(tasklink, termlink);
            float thisPri = priElseZero();
            if (thisPri > minPri) {
                p.pri(thisPri * (1f-momentum));
                ITask[] result = p.run(nar);
                if (result!=null) {
                    nar.input(result);
                    float cost = thisPri - p.priElseZero();
                    priSub(cost);
                }
                //premises.add(new Premise(tasklink, termlink));
            }
        }

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
