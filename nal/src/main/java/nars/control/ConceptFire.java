package nars.control;

import jcog.Util;
import jcog.bag.Bag;
import jcog.pri.PLink;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.premise.Premise;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class ConceptFire extends UnaryTask<Concept> {

    static final int MISFIRE_COST = 1;
    static final int premiseCost = 1;
    static final int linkSampleCost = 1;


    public ConceptFire(Concept c, float pri) {
        super(c, pri);
    }


    @Override
    public ITask[] run(NAR nar) {
        float pri = this.pri;
        if (pri!=pri)
            return null;

        int ttl = Util.lerp(pri, Param.FireTTLMax, Param.FireTTLMin);

        final Concept c = value;

        final Bag<Task, PLink<Task>> tasklinks = c.tasklinks().commit();//.normalize(0.1f);
        final Bag<Term, PLink<Term>> termlinks = c.termlinks().commit();//.normalize(0.1f);
        //nar.terms.commit(c);

        @Nullable PLink<Task> tasklink = null;
        @Nullable PLink<Term> termlink = null;
        float taskLinkPri = -1f, termLinkPri = -1f;

        List<PremiseBuilder> premises = $.newArrayList();
        //also maybe Set is appropriate here

        /**
         * this implements a pair of roulette wheel random selectors
         * which have their options weighted according to the normalized
         * termlink and tasklink priorities.  normalization allows the absolute
         * range to be independent which should be ok since it is only affecting
         * the probabilistic selection sequence and doesnt affect derivation
         * budgeting directly.
         */
        Random rng = nar.random();
        while (ttl > 0) {
            if (tasklink == null || (rng.nextFloat() > taskLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
                tasklink = tasklinks.sample();
                ttl -= linkSampleCost;
                if (tasklink == null)
                    break;

                taskLinkPri = tasklinks.normalizeMinMax(tasklink.priSafe(0));
            }


            if (termlink == null || (rng.nextFloat() > termLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
                termlink = termlinks.sample();
                ttl -= linkSampleCost;
                if (termlink == null)
                    break;
                termLinkPri = termlinks.normalizeMinMax(termlink.priSafe(0));
            }

            premises.add(new PremiseBuilder(tasklink, termlink));

            ttl -= premiseCost; //failure of premise generation still causes cost
        }

        //divide priority among the premises
        int num = premises.size();
        if (num > 0) {
            float subPri = pri / num;
            ITask[] pp = premises.toArray(new ITask[num]);
            for (ITask p : pp)
                p.setPri(subPri);
            return pp;
        } else {
            return null;
        }
    }

}
