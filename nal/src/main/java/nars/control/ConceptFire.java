package nars.control;

import jcog.Util;
import jcog.bag.Bag;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class ConceptFire extends UnaryTask<Concept> implements Termed {

    /** rate at which ConceptFire forms premises */
    private static final float spendRate = 0.25f;
    private static final int premisesPerCycle = 2;


    public ConceptFire(Concept c, float pri) {
        super(c, pri);
    }


    @Override
    public ITask[] run(NAR nar) {
        float pri = this.pri;
        if (pri!=pri)
            return null;

        final Concept c = id;

        final Bag<Task, PriReference<Task>> tasklinks = c.tasklinks().commit();//.normalize(0.1f);
        final Bag<Term, PriReference<Term>> termlinks = c.termlinks().commit();//.normalize(0.1f);
        nar.terms.commit(c); //index cache update

        @Nullable PriReference<Task> tasklink = null;
        @Nullable PriReference<Term> termlink = null;
        float taskLinkPri = -1f, termLinkPri = -1f;

        FasterList<Premise> premises = new FasterList<>(0,new Premise[premisesPerCycle]);
        //also maybe Set is appropriate here

        float taskMargin = 1f/(1+tasklinks.size());
        float termMargin = 1f/(1+termlinks.size());

        /**
         * this implements a pair of roulette wheel random selectors
         * which have their options weighted according to the normalized
         * termlink and tasklink priorities.  normalization allows the absolute
         * range to be independent which should be ok since it is only affecting
         * the probabilistic selection sequence and doesnt affect derivation
         * budgeting directly.
         */

        int ttl = premisesPerCycle;

        Random rng = nar.random();
        while (ttl > 0) {
            if (tasklink == null || (rng.nextFloat() > taskLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
                tasklink = tasklinks.sample();
                if (tasklink == null)
                    break;

                taskLinkPri = Util.clamp(tasklinks.normalizeMinMax(tasklink.priSafe(0)), taskMargin, 1f-taskMargin);
            }


            if (termlink == null || (rng.nextFloat() > termLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
                termlink = termlinks.sample();
                if (termlink == null)
                    break;
                termLinkPri = Util.clamp(termlinks.normalizeMinMax(termlink.priSafe(0)), termMargin, 1f-termMargin);
            }

            premises.add(new Premise(tasklink, termlink));
            ttl--;
        }

        int num = premises.size();
        if (num > 0) {
            ITask[] pp = premises.array();
            float spend = this.pri * spendRate;
            this.pri -= spend;

            //divide priority among the premises
            float subPri = spend / num;
            for (int i = 0; i < num; i++) {
                pp[i].setPri(subPri);
            }

            return pp;
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public final Term term() {
        return id.term();
    }

}
